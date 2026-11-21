package com.hellodoc.service;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DoctorApprovalEmailListenerTest {
    final DoctorApprovalEmailDeliveryService deliveries=mock(DoctorApprovalEmailDeliveryService.class);
    final DoctorApplicationApprovedEvent event=new DoctorApplicationApprovedEvent(UUID.randomUUID(),"doctor@example.test","Sami","Test");
    @Test void approvalThreadOnlyQueuesMailAndDoesNotWaitForSmtp(){
        List<Runnable> queue=new ArrayList<>();var listener=new DoctorApprovalEmailListener(deliveries,queue::add);
        listener.onApproved(event);assertEquals(1,queue.size());verifyNoInteractions(deliveries);
        queue.getFirst().run();verify(deliveries).deliver(event.applicationId());
    }
    @Test void saturatedExecutorDoesNotTurnCommittedApprovalIntoAnError(){
        var listener=new DoctorApprovalEmailListener(deliveries,task->{throw new RejectedExecutionException();});
        assertDoesNotThrow(()->listener.onApproved(event));verifyNoInteractions(deliveries);
    }
    @Test void backgroundFailureCannotPropagateIntoApprovalResponse(){
        doThrow(new IllegalStateException()).when(deliveries).deliver(any());
        var listener=new DoctorApprovalEmailListener(deliveries,Runnable::run);
        assertDoesNotThrow(()->listener.onApproved(event));
    }
}
