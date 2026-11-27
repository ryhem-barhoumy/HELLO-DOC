package com.hellodoc.service;

import com.hellodoc.entity.UserAccount;
import com.hellodoc.entity.UserRole;
import com.hellodoc.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PasswordRecoveryServiceTest {
    private static final String EMAIL = "patient@example.invalid";
    private static final String IP = "192.0.2.10";
    private static final String BROWSER = "browser-session";
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordRecoveryEmailSender mail = mock(PasswordRecoveryEmailSender.class);
    private final TestClock clock = new TestClock();
    private final PasswordRecoveryService recovery = new PasswordRecoveryService(users, mail, Runnable::run, clock);
    private final List<String> delivered = new ArrayList<>();

    PasswordRecoveryServiceTest() {
        when(mail.available()).thenReturn(true);
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(account("ACTIVE")));
        doAnswer(invocation -> delivered.add(invocation.getArgument(1))).when(mail).send(anyString(), anyString());
    }

    @Test
    void codeIsBoundToTheBrowserAndConsumedAfterOneSuccessfulVerification() {
        request();
        String code = delivered.getFirst();
        assertTrue(code.matches("[0-9]{6}"));
        expect(HttpStatus.BAD_REQUEST, () -> recovery.verify(EMAIL, code, IP, "different-browser"));
        assertEquals(UserRole.USER, recovery.verify(EMAIL, code, IP, BROWSER).getRole());
        expect(HttpStatus.BAD_REQUEST, () -> recovery.verify(EMAIL, code, IP, BROWSER));
    }

    @Test
    void expirationCooldownAndNewRequestInvalidateEarlierCodes() {
        request();
        String previous = delivered.getFirst();
        expect(HttpStatus.TOO_MANY_REQUESTS, this::request);
        clock.advance(Duration.ofMinutes(1));
        request();
        String replacement = delivered.getLast();
        assertNotEquals(previous, replacement);
        expect(HttpStatus.BAD_REQUEST, () -> recovery.verify(EMAIL, previous, IP, BROWSER));
        clock.advance(Duration.ofMinutes(5));
        expect(HttpStatus.BAD_REQUEST, () -> recovery.verify(EMAIL, replacement, IP, BROWSER));
    }

    @Test
    void repeatedIncorrectCodesExhaustTheChallenge() {
        request();
        String code = delivered.getFirst();
        String incorrect = code.equals("000000") ? "000001" : "000000";
        for (int attempt = 0; attempt < 5; attempt++) {
            expect(HttpStatus.BAD_REQUEST, () -> recovery.verify(EMAIL, incorrect, IP, BROWSER));
        }
        expect(HttpStatus.BAD_REQUEST, () -> recovery.verify(EMAIL, code, IP, BROWSER));
    }

    @Test
    void requestResponsesMatchForUnknownPendingAndActiveAccounts() {
        String pending = "pending@example.invalid";
        when(users.findByEmailIgnoreCase(pending)).thenReturn(Optional.of(account("PENDING")));
        var activeReceipt = request();
        assertEquals(activeReceipt, recovery.request("unknown@example.invalid", IP, () -> BROWSER));
        assertEquals(activeReceipt, recovery.request(pending, IP, () -> BROWSER));
        assertEquals(1, delivered.size());
    }

    @Test
    void verificationRechecksAccountStateAndCredentialVersion() {
        request();
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(account("INACTIVE")));
        expect(HttpStatus.BAD_REQUEST, () -> recovery.verify(EMAIL, delivered.getFirst(), IP, BROWSER));
        clock.advance(Duration.ofMinutes(1));
        var active = account("ACTIVE");
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(active));
        request();
        active.changePassword("replacement-hash");
        expect(HttpStatus.BAD_REQUEST, () -> recovery.verify(EMAIL, delivered.getLast(), IP, BROWSER));
    }

    @Test
    void emailQuotaCannotBeBypassedByChangingBrowserAndIp() {
        for (int count = 0; count < 4; count++) {
            request();
            clock.advance(Duration.ofMinutes(1));
        }
        expect(HttpStatus.TOO_MANY_REQUESTS,
                () -> recovery.request(EMAIL.toUpperCase(java.util.Locale.ROOT), "192.0.2.11", () -> "another-browser"));
    }

    @Test
    void missingSmtpConfigurationAndSendFailureNeverActivateAChallenge() {
        when(mail.available()).thenReturn(false);
        expect(HttpStatus.SERVICE_UNAVAILABLE, this::request);
        verify(mail, never()).send(anyString(), anyString());

        when(mail.available()).thenReturn(true);
        doAnswer(invocation -> {
            delivered.add(invocation.getArgument(1));
            throw new MailSendException("SMTP rejected test message");
        }).when(mail).send(eq(EMAIL), anyString());
        request();
        expect(HttpStatus.BAD_REQUEST, () -> recovery.verify(EMAIL, delivered.getFirst(), IP, BROWSER));
    }

    private PasswordRecoveryService.RequestReceipt request() {
        return recovery.request(EMAIL, IP, () -> BROWSER);
    }

    private static UserAccount account(String status) {
        return new UserAccount("Test", "Patient", EMAIL, "hash", UserRole.USER, status);
    }

    private static void expect(HttpStatus status, Runnable action) {
        assertEquals(status, assertThrows(ResponseStatusException.class, action::run).getStatusCode());
    }

    private static final class TestClock extends Clock {
        private Instant time = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration amount) { time = time.plus(amount); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(time, zone); }
        @Override public Instant instant() { return time; }
    }
}
