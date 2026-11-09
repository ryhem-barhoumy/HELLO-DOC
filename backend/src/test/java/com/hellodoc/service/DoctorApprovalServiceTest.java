package com.hellodoc.service;

import com.hellodoc.entity.*;
import com.hellodoc.repository.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DoctorApprovalServiceTest {
 @Test void approvalCreatesDoctorAndActivatesAccountAtomically(){
  DoctorApplicationRepository applications=mock(DoctorApplicationRepository.class);UserRepository users=mock(UserRepository.class);DoctorProfileService profiles=mock(DoctorProfileService.class);org.springframework.context.ApplicationEventPublisher events=mock(org.springframework.context.ApplicationEventPublisher.class);DoctorApprovalService service=new DoctorApprovalService(applications,users,profiles,events);
  UserAccount user=new UserAccount("D","Example","d@example.test","555","Clinic","hash",UserRole.USER,"PENDING");DoctorApplication app=mock(DoctorApplication.class);UUID id=UUID.randomUUID();
  when(applications.findById(id)).thenReturn(Optional.of(app));when(app.getStatus()).thenReturn("PENDING");when(app.hasDoctorDocument()).thenReturn(true);when(app.getUser()).thenReturn(user);when(app.getId()).thenReturn(id);when(app.getServiceTitle()).thenReturn("Dr D");when(app.getSpecialty()).thenReturn("Odontologie");when(app.getLicenseNumber()).thenReturn("LIC");when(app.getCity()).thenReturn("Tunis");when(app.getPracticeAddress()).thenReturn("Cabinet");doAnswer(i->{when(app.getStatus()).thenReturn(i.getArgument(0));return null;}).when(app).decide(any(),any());
  var response=service.approve(id,null);
  assertEquals(UserRole.DOCTOR,user.getRole());assertEquals("ACTIVE",user.getStatus());assertEquals("APPROVED",response.status());verify(profiles).createFromApplication(app);verify(app).decide("APPROVED",null);verify(users).save(user);verify(events).publishEvent(new DoctorApplicationApprovedEvent("d@example.test","D","Example"));
 }
}
