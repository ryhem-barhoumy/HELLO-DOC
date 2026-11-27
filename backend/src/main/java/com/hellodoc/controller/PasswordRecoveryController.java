package com.hellodoc.controller;

import com.hellodoc.dto.SessionUser;
import com.hellodoc.entity.UserAccount;
import com.hellodoc.repository.UserRepository;
import com.hellodoc.security.AuthenticatedSession;
import com.hellodoc.service.PasswordRecoveryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/auth/recovery")
public class PasswordRecoveryController {
    private static final String GRANT = PasswordRecoveryController.class.getName() + ".grant";
    private static final Duration PASSWORD_CHANGE_WINDOW = Duration.ofMinutes(5);
    private final PasswordRecoveryService recovery;
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final CookieCsrfTokenRepository csrfTokens = CookieCsrfTokenRepository.withHttpOnlyFalse();

    public PasswordRecoveryController(PasswordRecoveryService recovery, UserRepository users, PasswordEncoder passwords) {
        this.recovery = recovery;
        this.users = users;
        this.passwords = passwords;
    }

    @PostMapping("/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PasswordRecoveryService.RequestReceipt request(@Valid @RequestBody EmailRequest body, HttpServletRequest request) {
        return recovery.request(body.email(), request.getRemoteAddr(), () -> request.getSession(true).getId());
    }

    @PostMapping("/verify")
    public SessionUser verify(@Valid @RequestBody CodeRequest body, HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Demandez un code depuis cette fenêtre pour continuer.");
        }

        synchronized (session) {
            UserAccount user = recovery.verify(body.email(), body.code(), request.getRemoteAddr(), session.getId());
            var authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());
            var authentication = UsernamePasswordAuthenticationToken.authenticated(user.getEmail(), null, List.of(authority));
            AuthenticatedSession.establish(request, response, authentication, user);
            session.setAttribute(GRANT, new Grant(user.getEmail(), session.getId(), AuthenticatedSession.credentialVersion(user),
                    Instant.now().plus(PASSWORD_CHANGE_WINDOW)));
            rotateCsrf(request, response);
            return SessionUser.from(user);
        }
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody PasswordRequest body, Authentication authentication,
                               HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = requireSession(request);
        synchronized (session) {
            Grant grant = requireGrant(session, authentication);
            if (!Instant.now().isBefore(grant.expiresAt())) {
                throw expiredGrant();
            }
            if (!body.newPassword().equals(body.confirmPassword())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les mots de passe ne correspondent pas.");
            }
            if (body.newPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nouveau mot de passe doit tenir dans 72 octets UTF-8.");
            }

            UserAccount user = users.findByEmailIgnoreCase(grant.email())
                    .filter(account -> "ACTIVE".equals(account.getStatus())
                            && grant.credentialVersion().equals(AuthenticatedSession.credentialVersion(account)))
                    .orElseThrow(PasswordRecoveryController::expiredGrant);
            String replacement = passwords.encode(body.newPassword());
            if (users.replaceActivePassword(user.getId(), user.getPasswordHash(), replacement) != 1) {
                session.removeAttribute(GRANT);
                throw expiredGrant();
            }
            user.changePassword(replacement);
            session.removeAttribute(GRANT);
            AuthenticatedSession.establish(request, response, authentication, user);
            rotateCsrf(request, response);
        }
    }

    @PostMapping("/skip")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void skip(Authentication authentication, HttpServletRequest request) {
        HttpSession session = requireSession(request);
        synchronized (session) {
            requireGrant(session, authentication);
            session.removeAttribute(GRANT);
        }
    }

    private void rotateCsrf(HttpServletRequest request, HttpServletResponse response) {
        csrfTokens.saveToken(csrfTokens.generateToken(request), request, response);
    }

    private static HttpSession requireSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw expiredGrant();
        }
        return session;
    }

    private static Grant requireGrant(HttpSession session, Authentication authentication) {
        if (!(session.getAttribute(GRANT) instanceof Grant grant) || !grant.sessionId().equals(session.getId())
                || !grant.email().equalsIgnoreCase(authentication.getName())) {
            throw expiredGrant();
        }
        return grant;
    }

    private static ResponseStatusException expiredGrant() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Vérifiez un nouveau code pour autoriser la modification du mot de passe.");
    }

    private record Grant(String email, String sessionId, String credentialVersion, Instant expiresAt) { }

    public record EmailRequest(@NotBlank @Email @Size(max = 254) String email) { }

    public record CodeRequest(@NotBlank @Email @Size(max = 254) String email,
                              @NotBlank @Pattern(regexp = "[0-9]{6}") String code) {
        @Override public String toString() { return "CodeRequest[redacted]"; }
    }

    public record PasswordRequest(@NotBlank @Size(min = 8, max = 72) String newPassword,
                                  @NotBlank @Size(max = 72) String confirmPassword) {
        @Override public String toString() { return "PasswordRequest[redacted]"; }
    }
}
