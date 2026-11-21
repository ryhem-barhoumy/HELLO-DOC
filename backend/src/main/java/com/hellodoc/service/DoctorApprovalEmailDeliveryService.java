package com.hellodoc.service;

import com.hellodoc.dto.ApprovalEmailStatus;
import com.hellodoc.entity.DoctorApplication;
import com.hellodoc.entity.UserRole;
import com.hellodoc.repository.DoctorApplicationRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.UUID;

@Service
public class DoctorApprovalEmailDeliveryService {
    private static final System.Logger LOG=System.getLogger(DoctorApprovalEmailDeliveryService.class.getName());
    private final DoctorApplicationRepository applications;
    private final ApplicationEventPublisher events;
    private final ObjectProvider<DoctorApprovalEmailSender> senders;
    private final boolean enabled;
    public DoctorApprovalEmailDeliveryService(DoctorApplicationRepository applications,ApplicationEventPublisher events,
            ObjectProvider<DoctorApprovalEmailSender> senders,@Value("${hellodoc.email.enabled:false}") boolean enabled) {
        this.applications=applications;this.events=events;this.senders=senders;this.enabled=enabled;
    }
    @Transactional(readOnly=true)
    public ApprovalEmailStatus status(UUID id) {
        return ApprovalEmailStatus.from(applications.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Candidature introuvable.")));
    }
    @Transactional
    public ApprovalEmailStatus retry(UUID id) {
        DoctorApplication application=locked(id);
        if(!"APPROVED".equals(application.getStatus())||application.getUser().getRole()!=UserRole.DOCTOR||!"ACTIVE".equals(application.getUser().getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Le compte médecin doit être approuvé et actif avant l’envoi du courriel.");
        if("SENT".equals(application.getApprovalEmailStatus()))return ApprovalEmailStatus.from(application);
        application.queueApprovalEmail(enabled);
        if(enabled)events.publishEvent(event(application));
        return ApprovalEmailStatus.from(application);
    }
    /** A separate transaction commits delivery status after the approval has already committed. */
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void deliver(UUID id) {
        DoctorApplication application=locked(id);
        if(!"APPROVED".equals(application.getStatus())||!"QUEUED".equals(application.getApprovalEmailStatus()))return;
        if(!enabled){application.queueApprovalEmail(false);return;}
        try {
            DoctorApprovalEmailSender sender=senders.getIfAvailable();
            if(sender==null)throw new IllegalStateException("SMTP sender unavailable");
            sender.send(event(application));
            application.recordApprovalEmail("SENT",null);
            LOG.log(System.Logger.Level.INFO,"Courriel d’approbation transmis pour la candidature "+id);
        } catch(RuntimeException error) {
            String code=failureCode(error);
            application.recordApprovalEmail("FAILED",code);
            LOG.log(System.Logger.Level.ERROR,"Courriel d’approbation non envoyé pour la candidature "+id+" : "+code+". Le compte médecin reste approuvé.");
        }
    }
    private DoctorApplication locked(UUID id){return applications.findByIdForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Candidature introuvable."));}
    private DoctorApplicationApprovedEvent event(DoctorApplication a){var u=a.getUser();return new DoctorApplicationApprovedEvent(a.getId(),u.getEmail(),u.getFirstName(),u.getLastName());}
    static String failureCode(Throwable error) {
        for(Throwable current=error;current!=null;current=current.getCause()) {
            if(current instanceof MailAuthenticationException||current instanceof jakarta.mail.AuthenticationFailedException)return "AUTHENTICATION";
            if(current instanceof ConnectException||current instanceof SocketTimeoutException||current instanceof UnknownHostException)return "CONNECTION";
            if(current instanceof IllegalArgumentException||current instanceof IllegalStateException)return "CONFIGURATION";
        }
        return "DELIVERY";
    }
}
