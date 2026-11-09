package com.hellodoc.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(name="hellodoc.email.enabled",havingValue="true")
public class DoctorApprovalEmailListener {
    private static final System.Logger LOG=System.getLogger(DoctorApprovalEmailListener.class.getName());
    private final DoctorApprovalEmailSender emails;
    public DoctorApprovalEmailListener(DoctorApprovalEmailSender emails){this.emails=emails;}

    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void onApproved(DoctorApplicationApprovedEvent event) {
        try { emails.send(event); }
        catch(MailException ex) { LOG.log(System.Logger.Level.ERROR,"Échec d’envoi du courriel d’approbation médecin à "+event.email()+"; l’approbation reste enregistrée.",ex); }
    }
}
