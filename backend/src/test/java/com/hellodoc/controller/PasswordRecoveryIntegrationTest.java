package com.hellodoc.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellodoc.entity.UserAccount;
import com.hellodoc.entity.UserRole;
import com.hellodoc.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.regex.Pattern;
import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:hellodoc_recovery_test",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "hellodoc.admin.email=recovery-admin@example.invalid",
        "hellodoc.admin.initial-password=isolated-recovery-admin-password",
        "hellodoc.email.enabled=true",
        "hellodoc.email.from=hellodoc@example.invalid",
        "spring.mail.host=mail.example.invalid",
        "spring.mail.properties.mail.smtp.auth=false",
        "debug=false",
        "logging.level.root=WARN"
})
@AutoConfigureMockMvc
class PasswordRecoveryIntegrationTest {
    private static final String RECOVERY = "/api/auth/recovery";
    private static final String OLD_PASSWORD = "old-test-password";
    private static final String NEW_PASSWORD = "new-test-password";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired @Qualifier("passwordRecoveryExecutor") ThreadPoolTaskExecutor delivery;
    @MockitoBean JavaMailSender mail;

    @Test
    void verifiedOtpEstablishesSessionAndAResetRevokesOtherSessions() throws Exception {
        String email = account("recover-change@example.invalid", UserRole.USER);
        MockHttpSession normalSession = session(login(email, OLD_PASSWORD).andExpect(status().isOk()));
        mvc.perform(command(RECOVERY + "/password", passwordBody()).session(normalSession))
                .andExpect(status().isForbidden());

        MockHttpSession browser = requestCode(email);
        String originalId = browser.getId();
        String code = mailCode(email);
        mvc.perform(post(RECOVERY + "/verify").session(browser).contentType("application/json")
                        .content(json.writeValueAsString(Map.of("email", email, "code", code))))
                .andExpect(status().isForbidden());
        mvc.perform(command(RECOVERY + "/verify", Map.of("email", email, "code", code)).session(browser))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("USER"))
                .andExpect(cookie().exists("XSRF-TOKEN"));
        assertNotEquals(originalId, browser.getId());
        mvc.perform(get("/api/auth/me").session(browser)).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        String verifiedId = browser.getId();
        mvc.perform(command(RECOVERY + "/password", passwordBody()).session(browser))
                .andExpect(status().isNoContent());
        assertNotEquals(verifiedId, browser.getId());
        mvc.perform(get("/api/auth/me").session(normalSession)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SESSION_REVOKED"));
        mvc.perform(get("/api/auth/me").session(browser)).andExpect(status().isOk());
        mvc.perform(command("/api/auth/logout", Map.of()).session(browser)).andExpect(status().isNoContent());
        login(email, OLD_PASSWORD).andExpect(status().isUnauthorized());
        login(email, NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void verifiedDoctorCanSkipAndRetainTheOriginalPassword() throws Exception {
        String email = account("recover-skip@example.invalid", UserRole.DOCTOR);
        MockHttpSession browser = requestCode(email);
        String code = mailCode(email);
        mvc.perform(command(RECOVERY + "/verify", Map.of("email", email, "code", code)).session(browser))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("DOCTOR"));
        mvc.perform(command(RECOVERY + "/skip", Map.of()).session(browser)).andExpect(status().isNoContent());
        mvc.perform(command(RECOVERY + "/password", passwordBody()).session(browser))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/me").session(browser)).andExpect(status().isOk());
        mvc.perform(command("/api/auth/logout", Map.of()).session(browser)).andExpect(status().isNoContent());
        login(email, OLD_PASSWORD).andExpect(status().isOk());
    }

    private String account(String email, UserRole role) {
        users.save(new UserAccount("Test", "HelloDoc", email, passwords.encode(OLD_PASSWORD), role, "ACTIVE"));
        return email;
    }

    private MockHttpSession requestCode(String email) throws Exception {
        return session(mvc.perform(command(RECOVERY + "/request", Map.of("email", email)))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.expiresInSeconds").value(300))
                .andExpect(jsonPath("$.resendAfterSeconds").value(60)));
    }

    private String mailCode(String email) {
        var messages = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail, timeout(5000)).send(messages.capture());
        await().atMost(Duration.ofSeconds(5)).until(() -> delivery.getActiveCount() == 0);
        var message = messages.getValue();
        assertEquals("hellodoc@example.invalid", message.getFrom());
        assertArrayEquals(new String[]{email}, message.getTo());
        assertNotNull(message.getText());
        var code = Pattern.compile("\\b[0-9]{6}\\b").matcher(message.getText());
        assertTrue(code.find());
        assertFalse(message.getText().contains(OLD_PASSWORD));
        return code.group();
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(command("/api/auth/login", Map.of("email", email, "password", password)));
    }

    private MockHttpServletRequestBuilder command(String path, Map<String, String> body) throws Exception {
        var csrf = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk())
                .andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(csrf);
        return post(path).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
                .contentType("application/json").content(json.writeValueAsString(body));
    }

    private static MockHttpSession session(ResultActions response) {
        return (MockHttpSession) response.andReturn().getRequest().getSession(false);
    }

    private static Map<String, String> passwordBody() {
        return Map.of("newPassword", NEW_PASSWORD, "confirmPassword", NEW_PASSWORD);
    }
}
