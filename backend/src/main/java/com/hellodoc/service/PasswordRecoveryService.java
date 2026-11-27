package com.hellodoc.service;

import com.hellodoc.entity.UserAccount;
import com.hellodoc.entity.UserRole;
import com.hellodoc.repository.UserRepository;
import com.hellodoc.security.AuthenticatedSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

@Service
public class PasswordRecoveryService {
    private static final System.Logger LOG = System.getLogger(PasswordRecoveryService.class.getName());
    private static final Duration CODE_LIFETIME = Duration.ofMinutes(5);
    private static final Duration RESEND_DELAY = Duration.ofMinutes(1);
    private static final Duration LIMIT_WINDOW = Duration.ofMinutes(30);
    private static final int MAX_REQUESTS_PER_EMAIL = 4;
    private static final int MAX_REQUESTS_PER_IP = 12;
    private static final int MAX_CHECKS_PER_IP = 60;
    private static final int MAX_ATTEMPTS_PER_CODE = 5;
    private static final int MAX_TRACKED_ENTRIES = 10_000;

    private final UserRepository users;
    private final PasswordRecoveryEmailSender mail;
    private final Executor executor;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final SecretKeySpec key;
    private final Object lock = new Object();
    private final Map<String, Mailbox> mailboxes = new HashMap<>();
    private final Map<String, IpLimits> addresses = new HashMap<>();
    private Instant nextCleanup = Instant.EPOCH;

    @Autowired
    public PasswordRecoveryService(UserRepository users, PasswordRecoveryEmailSender mail,
                                   @Qualifier("passwordRecoveryExecutor") Executor executor) {
        this(users, mail, executor, Clock.systemUTC());
    }

    PasswordRecoveryService(UserRepository users, PasswordRecoveryEmailSender mail, Executor executor, Clock clock) {
        this.users = users;
        this.mail = mail;
        this.executor = executor;
        this.clock = clock;
        byte[] secret = new byte[32];
        random.nextBytes(secret);
        this.key = new SecretKeySpec(secret, "HmacSHA256");
    }

    public RequestReceipt request(String address, String clientIp, Supplier<String> browserSession) {
        if (!mail.available()) {
            throw unavailable();
        }
        String email = normalize(address);
        Instant now = clock.instant();
        Challenge challenge;
        synchronized (lock) {
            cleanExpired(now);
            IpLimits limits = ipLimits(clientIp, now);
            if (limits.requests >= MAX_REQUESTS_PER_IP) {
                throw throttled();
            }
            Mailbox mailbox = mailbox(email, now);
            if (mailbox.requests >= MAX_REQUESTS_PER_EMAIL || now.isBefore(mailbox.resendAt)) {
                throw throttled();
            }

            challenge = new Challenge(browserSession.get(), now.plus(CODE_LIFETIME));
            mailbox.challenge = challenge;
            mailbox.resendAt = now.plus(RESEND_DELAY);
            mailbox.requests++;
            limits.requests++;
        }
        try {
            // Every accepted address follows the same HTTP path, including unknown and disabled accounts.
            executor.execute(() -> deliver(email, challenge));
        } catch (RejectedExecutionException error) {
            discard(email, challenge);
            throw unavailable();
        }
        return new RequestReceipt(CODE_LIFETIME.toSeconds(), RESEND_DELAY.toSeconds());
    }

    public UserAccount verify(String address, String code, String clientIp, String browserSession) {
        String email = normalize(address);
        Instant now = clock.instant();
        AccountProof proof;
        synchronized (lock) {
            cleanExpired(now);
            IpLimits limits = ipLimits(clientIp, now);
            if (limits.checks >= MAX_CHECKS_PER_IP) {
                throw throttled();
            }
            limits.checks++;

            Mailbox mailbox = mailboxes.get(email);
            Challenge challenge = mailbox == null ? null : mailbox.challenge;
            if (challenge == null || !Objects.equals(browserSession, challenge.browserSession)
                    || !now.isBefore(challenge.expiresAt)) {
                throw invalid();
            }

            proof = challenge.proof;
            if (proof == null || !MessageDigest.isEqual(proof.code.digest, digest(challenge.id, code))) {
                if (--challenge.attempts == 0) {
                    mailbox.challenge = null;
                }
                throw invalid();
            }
            mailbox.challenge = null;
        }

        return users.findByEmailIgnoreCase(email)
                .filter(user -> "ACTIVE".equals(user.getStatus()) && user.getRole() == proof.role
                        && AuthenticatedSession.credentialVersion(user).equals(proof.credentialVersion))
                .orElseThrow(PasswordRecoveryService::invalid);
    }

    private void deliver(String email, Challenge challenge) {
        try {
            var user = users.findByEmailIgnoreCase(email).filter(account -> "ACTIVE".equals(account.getStatus()));
            if (user.isEmpty()) {
                return;
            }

            String code;
            IssuedCode issued;
            synchronized (lock) {
                if (!current(email, challenge)) {
                    return;
                }
                Mailbox mailbox = mailboxes.get(email);
                code = generateCode(mailbox.issuedCodes);
                issued = new IssuedCode(challenge.id, digest(challenge.id, code));
                mailbox.issuedCodes.add(issued);
            }

            UserAccount account = user.get();
            mail.send(account.getEmail(), code);
            synchronized (lock) {
                if (current(email, challenge)) {
                    challenge.proof = new AccountProof(issued, account.getRole(), AuthenticatedSession.credentialVersion(account));
                }
            }
        } catch (RuntimeException error) {
            discard(email, challenge);
            // SMTP exceptions can include message data or the destination address.
            LOG.log(System.Logger.Level.WARNING, "Échec d’envoi d’un code de récupération HelloDoc ({0}).",
                    error.getClass().getSimpleName());
        }
    }

    private String generateCode(List<IssuedCode> previousCodes) {
        String code;
        boolean repeated;
        do {
            code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
            String candidate = code;
            repeated = previousCodes.stream().anyMatch(previous -> MessageDigest.isEqual(previous.digest, digest(previous.id, candidate)));
        } while (repeated);
        return code;
    }

    private byte[] digest(UUID challenge, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return mac.doFinal((challenge + ":" + code).getBytes(StandardCharsets.US_ASCII));
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("HMAC-SHA-256 is unavailable", error);
        }
    }

    private boolean current(String email, Challenge challenge) {
        Mailbox mailbox = mailboxes.get(email);
        return mailbox != null && mailbox.challenge == challenge && clock.instant().isBefore(challenge.expiresAt);
    }

    private void discard(String email, Challenge challenge) {
        synchronized (lock) {
            Mailbox mailbox = mailboxes.get(email);
            if (mailbox != null && mailbox.challenge == challenge) {
                mailbox.challenge = null;
            }
        }
    }

    private Mailbox mailbox(String email, Instant now) {
        Mailbox mailbox = mailboxes.get(email);
        if (mailbox == null || !now.isBefore(mailbox.endsAt)) {
            if (mailbox == null && mailboxes.size() >= MAX_TRACKED_ENTRIES) {
                throw throttled();
            }
            mailbox = new Mailbox(now.plus(LIMIT_WINDOW), mailbox);
            mailboxes.put(email, mailbox);
        }
        return mailbox;
    }

    private IpLimits ipLimits(String address, Instant now) {
        IpLimits limits = addresses.get(address);
        if (limits == null || !now.isBefore(limits.endsAt)) {
            if (limits == null && addresses.size() >= MAX_TRACKED_ENTRIES) {
                throw throttled();
            }
            limits = new IpLimits(now.plus(LIMIT_WINDOW));
            addresses.put(address, limits);
        }
        return limits;
    }

    private void cleanExpired(Instant now) {
        if (now.isBefore(nextCleanup)) {
            return;
        }
        addresses.values().removeIf(limits -> !now.isBefore(limits.endsAt));
        mailboxes.values().removeIf(mailbox -> !now.isBefore(mailbox.endsAt) && !now.isBefore(mailbox.resendAt)
                && (mailbox.challenge == null || !now.isBefore(mailbox.challenge.expiresAt)));
        nextCleanup = now.plus(RESEND_DELAY);
    }

    private static String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private static ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Le code est incorrect, expiré ou a déjà été utilisé. Demandez un nouveau code si nécessaire.");
    }

    private static ResponseStatusException throttled() {
        return new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Trop de demandes ou de tentatives. Patientez avant de réessayer.");
    }

    private static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "L’envoi de codes est momentanément indisponible. Réessayez plus tard.");
    }

    public record RequestReceipt(long expiresInSeconds, long resendAfterSeconds) { }

    private record AccountProof(IssuedCode code, UserRole role, String credentialVersion) { }
    private record IssuedCode(UUID id, byte[] digest) { }

    private static final class Challenge {
        private final UUID id = UUID.randomUUID();
        private final String browserSession;
        private final Instant expiresAt;
        private int attempts = MAX_ATTEMPTS_PER_CODE;
        private AccountProof proof;

        private Challenge(String browserSession, Instant expiresAt) {
            this.browserSession = browserSession;
            this.expiresAt = expiresAt;
        }
    }

    private static final class Mailbox {
        private final Instant endsAt;
        private final List<IssuedCode> issuedCodes = new ArrayList<>();
        private Instant resendAt = Instant.EPOCH;
        private int requests;
        private Challenge challenge;

        private Mailbox(Instant endsAt, Mailbox previous) {
            this.endsAt = endsAt;
            if (previous != null) {
                resendAt = previous.resendAt;
                challenge = previous.challenge;
                if (!previous.issuedCodes.isEmpty()) {
                    issuedCodes.add(previous.issuedCodes.getLast());
                }
            }
        }
    }

    private static final class IpLimits {
        private final Instant endsAt;
        private int requests;
        private int checks;

        private IpLimits(Instant endsAt) {
            this.endsAt = endsAt;
        }
    }
}
