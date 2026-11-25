package com.hellodoc.service;

import com.hellodoc.entity.*;
import com.hellodoc.repository.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PatientClinicalServiceTest {
 @Test void patientResponseMapsSharedFieldsWithoutPrivateNote(){
  UserRepository users=mock(UserRepository.class);AppointmentRepository appointments=mock(AppointmentRepository.class);ConsultationNoteRepository notes=mock(ConsultationNoteRepository.class);PrescriptionRepository rx=mock(PrescriptionRepository.class);
  UserAccount patientUser=new UserAccount("Pat","Ient","patient@test.invalid","hash",UserRole.USER,"ACTIVE");UserAccount doctorUser=new UserAccount("Doc","Tor","doctor@test.invalid","hash",UserRole.DOCTOR,"ACTIVE");Doctor doctor=mock(Doctor.class);when(doctor.getUser()).thenReturn(doctorUser);Patient patient=new Patient(patientUser);
  Appointment visit=new Appointment(doctor,patient,"Consultation","50",Instant.now().minusSeconds(7200),Instant.now().minusSeconds(3600),null);
  ConsultationNote note=new ConsultationNote(visit);note.update("Résumé partagé","Observations","Traitement",LocalDate.now().plusDays(2),"dans deux semaines","Revoir si les symptômes persistent","NOTE PRIVÉE CONFIDENTIELLE");
  note.finalizeReport();when(users.findByEmailIgnoreCase("patient@test.invalid")).thenReturn(Optional.of(patientUser));when(appointments.findPatientCompletedByUserId(patientUser.getId())).thenReturn(List.of(visit));when(notes.findByAppointmentIdIn(anyList())).thenReturn(List.of(note));when(rx.findByAppointmentIdInOrderByPrescribedAtDesc(anyList())).thenReturn(List.of());
  var rows=new PatientClinicalService(users,appointments,notes,rx).myConsultations("patient@test.invalid");
  assertEquals(1,rows.size());assertEquals("Résumé partagé",rows.get(0).note().resume());assertEquals("dans deux semaines",rows.get(0).note().recommendedDelay());assertEquals("Revoir si les symptômes persistent",rows.get(0).note().followUpInstructions());
  assertFalse(Arrays.stream(rows.get(0).note().getClass().getRecordComponents()).anyMatch(c->c.getName().equalsIgnoreCase("privateNote")));
  assertFalse(rows.get(0).note().toString().contains("CONFIDENTIEL"));verify(appointments).findPatientCompletedByUserId(patientUser.getId());
 }
 @Test void patientDoesNotReceiveUnfinalizedDraft(){
  var users=mock(UserRepository.class);var appointments=mock(AppointmentRepository.class);var notes=mock(ConsultationNoteRepository.class);var rx=mock(PrescriptionRepository.class);var user=new UserAccount("Pat","Ient","patient-draft@test.invalid","hash",UserRole.USER,"ACTIVE");var doctor=mock(Doctor.class);when(doctor.getUser()).thenReturn(new UserAccount("Doc","Tor","doctor@test.invalid","hash",UserRole.DOCTOR,"ACTIVE"));var visit=new Appointment(doctor,new Patient(user),"Consultation","50",Instant.now().minusSeconds(7200),Instant.now().minusSeconds(3600),null);var draft=new ConsultationNote(visit);draft.update("Brouillon secret","","",null,"Note privée");when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));when(appointments.findPatientCompletedByUserId(user.getId())).thenReturn(List.of(visit));when(notes.findByAppointmentIdIn(anyList())).thenReturn(List.of(draft));when(rx.findByAppointmentIdInOrderByPrescribedAtDesc(anyList())).thenReturn(List.of());var result=new PatientClinicalService(users,appointments,notes,rx).myConsultations(user.getEmail());assertNull(result.get(0).note());assertFalse(result.toString().contains("Brouillon secret"));assertFalse(result.toString().contains("Note privée"));
 }
 @Test void inactiveOrUnknownAccountCannotReadPatientClinicalData(){
  UserRepository users=mock(UserRepository.class);var service=new PatientClinicalService(users,mock(AppointmentRepository.class),mock(ConsultationNoteRepository.class),mock(PrescriptionRepository.class));
  when(users.findByEmailIgnoreCase("inactive@test.invalid")).thenReturn(Optional.of(new UserAccount("In","Active","inactive@test.invalid","hash",UserRole.USER,"INACTIVE")));
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->service.myConsultations("inactive@test.invalid"));
 }
}
