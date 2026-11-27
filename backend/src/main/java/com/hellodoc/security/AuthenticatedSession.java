package com.hellodoc.security;

import com.hellodoc.entity.UserAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class AuthenticatedSession {
    private static final String CREDENTIAL_VERSION = AuthenticatedSession.class.getName() + ".credentialVersion";
    private static final HttpSessionSecurityContextRepository CONTEXTS = new HttpSessionSecurityContextRepository();

    private AuthenticatedSession() { }

    public static HttpSession establish(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication, UserAccount user) {
        var session = request.getSession(true);
        request.changeSessionId();
        session.setAttribute(CREDENTIAL_VERSION, credentialVersion(user));
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        CONTEXTS.saveContext(context, request, response);
        return session;
    }

    public static boolean matchesCredentials(HttpSession session, UserAccount user) {
        var version = session == null ? null : session.getAttribute(CREDENTIAL_VERSION);
        return version == null || version.equals(credentialVersion(user));
    }

    public static String credentialVersion(UserAccount user) {
        try {
            var bytes = (user.getId() + ":" + user.getPasswordHash()).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }
}
