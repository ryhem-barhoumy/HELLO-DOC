package com.hellodoc.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PasswordRecoveryEmailSender {
    private final ObjectProvider<JavaMailSender> mail;
    private final boolean configured;
    private final String from;

    public PasswordRecoveryEmailSender(ObjectProvider<JavaMailSender> mail,
            @Value("${hellodoc.email.enabled:false}") boolean enabled,
            @Value("${hellodoc.email.from:}") String from,
            @Value("${spring.mail.host:}") String host,
            @Value("${spring.mail.properties.mail.smtp.auth:true}") boolean authentication,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password) {
        this.mail = mail;
        this.from = from;
        configured = enabled && StringUtils.hasText(from) && StringUtils.hasText(host)
                && (!authentication || (StringUtils.hasText(username) && StringUtils.hasText(password)));
    }

    public boolean available() {
        return configured && mail.getIfAvailable() != null;
    }

    public void send(String email, String code) {
        var message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("Votre code de connexion HelloDoc");
        message.setText("""
                Bonjour,

                Voici votre code de vérification HelloDoc : %s

                Ce code est valable cinq minutes à compter de votre demande et ne peut être utilisé qu’une fois.
                Saisissez-le dans la fenêtre HelloDoc où vous avez demandé la récupération.

                Gardez ce code confidentiel. L’équipe HelloDoc ne vous le demandera jamais.
                Si vous n’avez pas demandé de code, vous pouvez ignorer ce message.

                L’équipe HelloDoc
                """.formatted(code));
        mail.getObject().send(message);
    }
}
