package com.hellodoc.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import static org.junit.jupiter.api.Assertions.fail;

/** Explicit opt-in: authenticates to the configured server, but sends no email. */
@EnabledIfEnvironmentVariable(named="HELLODOC_SMTP_LIVE_TEST",matches="true")
class DoctorApprovalSmtpConnectionTest {
    @Test void authenticatesWithConfiguredAppPasswordWithoutSendingMail() {
        JavaMailSenderImpl sender=new JavaMailSenderImpl();
        sender.setHost(System.getenv("HELLODOC_SMTP_HOST"));
        sender.setPort(Integer.parseInt(System.getenv().getOrDefault("HELLODOC_SMTP_PORT","587")));
        sender.setUsername(System.getenv("HELLODOC_SMTP_USERNAME"));sender.setPassword(System.getenv("HELLODOC_SMTP_PASSWORD"));
        var properties=sender.getJavaMailProperties();properties.put("mail.smtp.auth","true");properties.put("mail.smtp.starttls.enable","true");
        properties.put("mail.smtp.connectiontimeout","5000");properties.put("mail.smtp.timeout","10000");properties.put("mail.smtp.writetimeout","10000");
        new DoctorApprovalEmailSender(sender,System.getenv("HELLODOC_EMAIL_FROM"),"http://localhost:4200",sender.getHost());
        try { sender.testConnection(); }
        catch(Exception error){fail("SMTP connection check failed: "+DoctorApprovalEmailDeliveryService.failureCode(error)+" (no credentials logged)");}
    }
}
