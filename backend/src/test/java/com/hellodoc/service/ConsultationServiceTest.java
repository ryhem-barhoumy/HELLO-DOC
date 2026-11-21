package com.hellodoc.service;
import com.hellodoc.dto.*;
import com.hellodoc.entity.*;
import com.hellodoc.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ConsultationServiceTest {
 static class Fixture {
  UUID id=UUID.randomUUID();String email="doctor@qa.test";
  AppointmentRepository appointments=mock(AppointmentRepository.class);ConsultationNoteRepository notes=mock(ConsultationNoteRepository.class);ConsultationRevisionRepository revisions=mock(ConsultationRevisionRepository.class);DoctorProfileService profiles=mock(DoctorProfileService.class);PatientRepository patients=mock(PatientRepository.class);NotificationService notifications=mock(NotificationService.class);
  Doctor doctor=mock(Doctor.class);UserAccount doctorUser=new UserAccount("Docteur","Test",email,"hash",UserRole.DOCTOR,"ACTIVE");Patient patient=new Patient(new UserAccount("Patient","Test","patient@qa.test","hash",UserRole.USER,"ACTIVE"));
  Appointment appointment=new Appointment(doctor,patient,"Consultation","50 DT",Instant.now().minusSeconds(7200),Instant.now().minusSeconds(5400),null);
  ConsultationService service=new ConsultationService(appointments,notes,revisions,profiles,patients,notifications);
  Fixture(){when(doctor.getUser()).thenReturn(doctorUser);when(profiles.own(email)).thenReturn(doctor);when(appointments.findDoctorOwned(id,email)).thenReturn(Optional.of(appointment));when(appointments.lockDoctorOwned(id,email)).thenReturn(Optional.of(appointment));when(notes.findByAppointmentId(id)).thenReturn(Optional.empty());appointment.changeStatus(AppointmentStatus.COMPLETED,null);}
  ConsultationNoteRequest request(Long version,LocalDate date){return new ConsultationNoteRequest("Résumé clinique","Observation","Recommandations",date,"Note privée",version);}
 }
 @Test void unrelatedDoctorCannotReadOrWriteEvenKnowingAnAppointmentId(){var f=new Fixture();when(f.appointments.findDoctorOwned(f.id,f.email)).thenReturn(Optional.empty());when(f.appointments.lockDoctorOwned(f.id,f.email)).thenReturn(Optional.empty());assertEquals(404,assertThrows(ResponseStatusException.class,()->f.service.note(f.email,f.id)).getStatusCode().value());assertEquals(404,assertThrows(ResponseStatusException.class,()->f.service.save(f.email,f.id,f.request(null,null))).getStatusCode().value());verifyNoInteractions(f.notes,f.revisions,f.notifications);}
 @Test void pendingConsultationHasNoClinicalRecord(){var f=new Fixture();f.appointment.changeStatus(AppointmentStatus.PENDING,null);assertEquals(409,assertThrows(ResponseStatusException.class,()->f.service.save(f.email,f.id,f.request(null,null))).getStatusCode().value());verifyNoInteractions(f.notes,f.revisions,f.notifications);}
 @Test void firstSaveCreatesRevisionAndFollowUpDoesNotExposeClinicalText(){var f=new Fixture();LocalDate next=LocalDate.now(ZoneId.of("Africa/Tunis")).plusDays(3);var saved=f.service.save(f.email,f.id,f.request(null,next));assertEquals("Résumé clinique",saved.resume());verify(f.notes).saveAndFlush(any(ConsultationNote.class));verify(f.revisions).save(any(ConsultationRevision.class));verify(f.notifications).create(eq(f.patient.getUser()),eq(f.appointment),eq("FOLLOW_UP_RECOMMENDED"),eq("Contrôle conseillé"),argThat(message->!message.contains("clinique")&&!message.contains("privée")&&message.contains(next.toString())));}
 @Test void staleOrMissingVersionCannotOverwriteExistingText(){var f=new Fixture();var note=new ConsultationNote(f.appointment);note.update("Version à conserver","","",null,"");org.springframework.test.util.ReflectionTestUtils.setField(note,"version",3L);when(f.notes.findByAppointmentId(f.id)).thenReturn(Optional.of(note));for(Long version:Arrays.asList(null,2L)){assertEquals(409,assertThrows(ResponseStatusException.class,()->f.service.save(f.email,f.id,f.request(version,null))).getStatusCode().value());}assertEquals("Version à conserver",note.getResume());verify(f.notes,never()).saveAndFlush(any());verifyNoInteractions(f.notifications,f.revisions);}
 @Test void unchangedFollowUpDoesNotSendRepeatedNotifications(){var f=new Fixture();var note=new ConsultationNote(f.appointment);LocalDate date=LocalDate.now(ZoneId.of("Africa/Tunis")).plusDays(2);note.update("Ancien résumé","","",date,"");when(f.notes.findByAppointmentId(f.id)).thenReturn(Optional.of(note));f.service.save(f.email,f.id,f.request(0L,date));verifyNoInteractions(f.notifications);verify(f.revisions).save(any());}
 @Test void doctorWithoutCompletedVisitCannotDiscoverPatientIdentity(){var f=new Fixture();when(f.doctor.getId()).thenReturn(UUID.randomUUID());assertEquals(404,assertThrows(ResponseStatusException.class,()->f.service.dossier(f.email,UUID.randomUUID(),0,10)).getStatusCode().value());verifyNoInteractions(f.patients,f.notes);}
 @Test void pastFollowUpIsRejectedBeforePersisting(){var f=new Fixture();assertEquals(400,assertThrows(ResponseStatusException.class,()->f.service.save(f.email,f.id,f.request(null,LocalDate.now(ZoneId.of("Africa/Tunis"))))).getStatusCode().value());verify(f.notes,never()).saveAndFlush(any());verifyNoInteractions(f.revisions,f.notifications);}
 @Test void doctorCanUpdateHistoricalRecordWithoutChangingAnOldFollowUpDate(){var f=new Fixture();var note=new ConsultationNote(f.appointment);LocalDate past=LocalDate.now(ZoneId.of("Africa/Tunis")).minusDays(3);note.update("Historique","","",past,"");when(f.notes.findByAppointmentId(f.id)).thenReturn(Optional.of(note));assertEquals(past,f.service.save(f.email,f.id,f.request(0L,past)).nextCheck());verifyNoInteractions(f.notifications);}
}
