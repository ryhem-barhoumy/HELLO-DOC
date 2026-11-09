package com.hellodoc.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name="hellodoc.email.enabled",havingValue="true")
public class DoctorApprovalEmailSender {
    private static final System.Logger LOG=System.getLogger(DoctorApprovalEmailSender.class.getName());
    private final JavaMailSender mailSender;
    private final String from;
    private final String frontendUrl;
    private final String smtpHost;

    public DoctorApprovalEmailSender(JavaMailSender mailSender,
            @Value("${hellodoc.email.from:no-reply@hellodoc.tn}") String from,
            @Value("${hellodoc.frontend-url:http://localhost:4200}") String frontendUrl,
            @Value("${spring.mail.host:}") String smtpHost) {
        this.mailSender=mailSender;this.from=from;this.frontendUrl=frontendUrl;this.smtpHost=smtpHost;
    }

    public void send(DoctorApplicationApprovedEvent event) {
        if(smtpHost==null||smtpHost.isBlank()) {
            LOG.log(System.Logger.Level.ERROR,"Courriel d’approbation non envoyé : HELLODOC_SMTP_HOST n’est pas configuré.");
            return;
        }
        String greeting=(event.firstName()==null||event.firstName().isBlank())?"Docteur":"Docteur "+event.firstName().trim();
        String safeUrl=frontendUrl.endsWith("/")?frontendUrl.substring(0,frontendUrl.length()-1):frontendUrl;
        SimpleMailMessage message=new SimpleMailMessage();
        message.setFrom(from);message.setTo(event.email());
        message.setSubject("Votre inscription médecin Hello Doc est approuvée");
        message.setText("Bonjour "+greeting+",\n\n"
                +"Nous avons le plaisir de vous informer que votre demande d’inscription en tant que médecin sur Hello Doc a été approuvée par notre équipe.\n\n"
                +"Votre compte professionnel est maintenant activé. Vous pouvez accéder à votre espace médecin en vous connectant avec l’adresse e-mail et le mot de passe choisis lors de votre inscription :\n"
                +safeUrl+"/connexion\n\n"
                +"Pour votre sécurité, Hello Doc ne vous demandera jamais de communiquer votre mot de passe par e-mail.\n\n"
                +"Cordialement,\nL’équipe Hello Doc");
        mailSender.send(message);
    }
}
