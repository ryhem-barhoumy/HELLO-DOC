package com.hellodoc.service;

import com.hellodoc.entity.*;
import com.hellodoc.repository.DoctorApplicationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DoctorApprovalEmailDeliveryServiceTest {
    final UUID id=UUID.randomUUID();
    final DoctorApplicationRepository applications=mock(DoctorApplicationRepository.class);
    final ApplicationEventPublisher events=mock(ApplicationEventPublisher.class);
    final DoctorApprovalEmailSender sender=mock(DoctorApprovalEmailSender.class);
    @SuppressWarnings("unchecked") final ObjectProvider<DoctorApprovalEmailSender> senders=mock(ObjectProvider.class);
    DoctorApplication application() {
        UserAccount user=new UserAccount("Sami","Test","doctor@example.test","hash",UserRole.DOCTOR,"ACTIVE");
        DoctorApplication a=new DoctorApplication(user,"Dr Sami","Cardiologie","ORDRE","Tunis","Cabinet",30,"","",0,"[]","[]","");
        org.springframework.test.util.ReflectionTestUtils.setField(a,"id",id);
        a.decide("APPROVED",null);a.queueApprovalEmail(true);
        when(applications.findByIdForUpdate(id)).thenReturn(Optional.of(a));
        when(applications.findById(id)).thenReturn(Optional.of(a));
        when(senders.getIfAvailable()).thenReturn(sender);
        return a;
    }
    DoctorApprovalEmailDeliveryService service(boolean enabled){return new DoctorApprovalEmailDeliveryService(applications,events,senders,enabled);}
    @Test void successfulSendIsPersistedAndRepeatedWorkerDoesNotSendTwice(){
        var a=application();var service=service(true);
        service.deliver(id);service.deliver(id);
        assertEquals("SENT",a.getApprovalEmailStatus());assertNotNull(a.getApprovalEmailSentAt());
        verify(sender,times(1)).send(any());assertEquals("ACTIVE",a.getUser().getStatus());
    }
    @Test void smtpFailureDoesNotUndoApprovalAndCanBeRetriedWithoutRecreatingDoctor(){
        var a=application();var service=service(true);
        doThrow(new MailAuthenticationException("authentication refused")).when(sender).send(any());
        service.deliver(id);
        assertEquals("FAILED",a.getApprovalEmailStatus());assertEquals("AUTHENTICATION",a.getApprovalEmailError());
        assertEquals("APPROVED",a.getStatus());assertEquals(UserRole.DOCTOR,a.getUser().getRole());assertEquals("ACTIVE",a.getUser().getStatus());
        assertTrue(service.status(id).message().contains("mot de passe d’application"));
        assertEquals("QUEUED",service.retry(id).status());verify(events).publishEvent(new DoctorApplicationApprovedEvent(id,"doctor@example.test","Sami","Test"));
        doNothing().when(sender).send(any());service.deliver(id);assertEquals("SENT",a.getApprovalEmailStatus());
    }
    @Test void alreadySentRetryDoesNotQueueDuplicateEmail(){
        var a=application();a.recordApprovalEmail("SENT",null);
        assertEquals("SENT",service(true).retry(id).status());verifyNoInteractions(events,sender);
    }
    @Test void disabledEmailIsReportedHonestly(){
        var a=application();assertEquals("DISABLED",service(false).retry(id).status());verifyNoInteractions(events,sender);
        assertEquals("ACTIVE",a.getUser().getStatus());
    }
    @Test void pendingOrInactiveDoctorCannotRequestApprovalMail(){
        var a=application();a.decide("PENDING",null);
        assertEquals(409,assertThrows(ResponseStatusException.class,()->service(true).retry(id)).getStatusCode().value());
        a.decide("APPROVED",null);a.getUser().setStatus("INACTIVE");
        assertThrows(ResponseStatusException.class,()->service(true).retry(id));verifyNoInteractions(events,sender);
    }
    @Test void networkAndConfigurationFailuresHaveActionableMessages(){
        assertEquals("CONNECTION",DoctorApprovalEmailDeliveryService.failureCode(new RuntimeException(new java.net.SocketTimeoutException())));
        assertEquals("CONFIGURATION",DoctorApprovalEmailDeliveryService.failureCode(new IllegalStateException()));
        var a=application();when(senders.getIfAvailable()).thenReturn(null);service(true).deliver(id);
        assertEquals("FAILED",a.getApprovalEmailStatus());assertEquals("CONFIGURATION",a.getApprovalEmailError());
    }
}
