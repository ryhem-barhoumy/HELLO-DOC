package com.hellodoc.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(name="hellodoc.email.enabled",havingValue="true")
public class DoctorApprovalEmailListener {
    private static final System.Logger LOG=System.getLogger(DoctorApprovalEmailListener.class.getName());
    private final DoctorApprovalEmailDeliveryService emails;
    private final Executor executor;
    public DoctorApprovalEmailListener(DoctorApprovalEmailDeliveryService emails,@Qualifier("doctorApprovalEmailExecutor") Executor executor){this.emails=emails;this.executor=executor;}

    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void onApproved(DoctorApplicationApprovedEvent event) {
        try { executor.execute(()->{
            try { emails.deliver(event.applicationId()); }
            catch(RuntimeException error){LOG.log(System.Logger.Level.ERROR,"Traitement du courriel interrompu pour la candidature "+event.applicationId()+" ; son envoi peut être relancé depuis l’administration.");}
        }); }
        catch(RuntimeException error){LOG.log(System.Logger.Level.ERROR,"Envoi différé pour la candidature "+event.applicationId()+" ; son envoi peut être relancé depuis l’administration.");}
    }
}
