package com.hellodoc.service;

import com.hellodoc.entity.*;
import com.hellodoc.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TeleconsultationServiceTest {
 @Test void appointmentAccessWindowIncludesEarlyEntryAndThirtyMinuteGrace(){Instant start=Instant.parse("2030-01-01T18:00:00Z");assertTrue(TeleconsultationService.withinAccessWindow(start,start.minusSeconds(900)));assertTrue(TeleconsultationService.withinAccessWindow(start,start));assertTrue(TeleconsultationService.withinAccessWindow(start,start.plusSeconds(1800)));assertFalse(TeleconsultationService.withinAccessWindow(start,start.minusSeconds(901)));assertFalse(TeleconsultationService.withinAccessWindow(start,start.plusSeconds(1801)));}
 @Test void inPersonAppointmentCannotCreateVideoSession(){
  var sessions=mock(TeleconsultationSessionRepository.class);var appointments=mock(AppointmentRepository.class);var users=mock(UserRepository.class);var doctors=mock(DoctorProfileService.class);
  var doctorUser=new UserAccount("Doc","Tor","doctor@test.invalid","hash",UserRole.DOCTOR,"ACTIVE");var doctor=mock(Doctor.class);when(doctor.getUser()).thenReturn(doctorUser);when(doctors.own(doctorUser.getEmail())).thenReturn(doctor);
  var patient=new Patient(new UserAccount("Pat","Ient","patient@test.invalid","hash",UserRole.USER,"ACTIVE"));var appointment=new Appointment(doctor,patient,"Consultation","50",Instant.now().minusSeconds(1200),Instant.now().plusSeconds(1200),null,ConsultationMode.IN_PERSON);appointment.changeStatus(AppointmentStatus.CONFIRMED,null);UUID id=UUID.randomUUID();when(sessions.lockDoctorAppointment(id,doctorUser.getEmail())).thenReturn(Optional.of(appointment));
  var error=assertThrows(ResponseStatusException.class,()->new TeleconsultationService(sessions,appointments,users,doctors).create(doctorUser.getEmail(),id));
  assertEquals(409,error.getStatusCode().value());verify(sessions,never()).save(any());verify(sessions,never()).findByAppointmentId(any());
 }
 @Test void repeatedCreateReturnsSameAppointmentSession(){
  var sessions=mock(TeleconsultationSessionRepository.class);var appointments=mock(AppointmentRepository.class);var users=mock(UserRepository.class);var doctors=mock(DoctorProfileService.class);
  var doctorUser=new UserAccount("Doc","Tor","doctor@test.invalid","hash",UserRole.DOCTOR,"ACTIVE");var doctor=mock(Doctor.class);when(doctor.getUser()).thenReturn(doctorUser);
  var patient=new Patient(new UserAccount("Pat","Ient","patient@test.invalid","hash",UserRole.USER,"ACTIVE"));var appointment=new Appointment(doctor,patient,"Consultation","50",Instant.now().minusSeconds(1200),Instant.now().plusSeconds(1200),null,ConsultationMode.VIDEO);appointment.changeStatus(AppointmentStatus.CONFIRMED,null);
  var existing=new TeleconsultationSession(appointment);when(doctors.own("doctor@test.invalid")).thenReturn(doctor);when(sessions.lockDoctorAppointment(any(),eq("doctor@test.invalid"))).thenReturn(Optional.of(appointment));when(sessions.findByAppointmentId(any())).thenReturn(Optional.of(existing));
  var service=new TeleconsultationService(sessions,appointments,users,doctors);var a=service.create("doctor@test.invalid",UUID.randomUUID());var b=service.create("doctor@test.invalid",UUID.randomUUID());
  assertEquals(a,b);verify(sessions,never()).save(any());verify(sessions,times(2)).findByAppointmentId(any());
 }
 @Test void thirdPartyCannotReadSessionEvenWithItsId(){
  var sessions=mock(TeleconsultationSessionRepository.class);var users=mock(UserRepository.class);var doctors=mock(DoctorProfileService.class);var appts=mock(AppointmentRepository.class);
  var doctorUser=new UserAccount("Doc","Tor","doctor@test.invalid","hash",UserRole.DOCTOR,"ACTIVE");var doctor=mock(Doctor.class);when(doctor.getUser()).thenReturn(doctorUser);var patientUser=new UserAccount("Pat","Ient","patient@test.invalid","hash",UserRole.USER,"ACTIVE");var patient=new Patient(patientUser);var appointment=new Appointment(doctor,patient,"Consultation","50",Instant.now(),Instant.now().plusSeconds(1200),null,ConsultationMode.VIDEO);appointment.changeStatus(AppointmentStatus.CONFIRMED,null);var session=new TeleconsultationSession(appointment);UUID id=UUID.randomUUID();
  when(users.findByEmailIgnoreCase("stranger@test.invalid")).thenReturn(Optional.of(new UserAccount("Out","Sider","stranger@test.invalid","hash",UserRole.USER,"ACTIVE")));when(sessions.findParticipantContext(id)).thenReturn(Optional.of(session));
  assertThrows(ResponseStatusException.class,()->new TeleconsultationService(sessions,appts,users,doctors).get("stranger@test.invalid",id));
 }
 @Test void completedSessionCannotBeRestarted(){var appointment=mock(Appointment.class);var session=new TeleconsultationSession(appointment);session.complete();assertThrows(IllegalStateException.class,session::start);}
 @Test void patientCannotStartOrCompleteTheDoctorsSession(){
  var sessions=mock(TeleconsultationSessionRepository.class);var users=mock(UserRepository.class);var id=UUID.randomUUID();var doctorUser=new UserAccount("Doc","Tor","doctor@test.invalid","hash",UserRole.DOCTOR,"ACTIVE");var doctor=mock(Doctor.class);when(doctor.getUser()).thenReturn(doctorUser);var patientUser=new UserAccount("Pat","Ient","patient@test.invalid","hash",UserRole.USER,"ACTIVE");org.springframework.test.util.ReflectionTestUtils.setField(patientUser,"id",UUID.randomUUID());var appointment=new Appointment(doctor,new Patient(patientUser),"Consultation","50",Instant.now().minusSeconds(10),Instant.now().plusSeconds(1200),null,ConsultationMode.VIDEO);appointment.changeStatus(AppointmentStatus.CONFIRMED,null);var session=new TeleconsultationSession(appointment);
  when(users.findByEmailIgnoreCase("patient@test.invalid")).thenReturn(Optional.of(patientUser));when(sessions.findParticipantContext(id)).thenReturn(Optional.of(session));var service=new TeleconsultationService(sessions,mock(AppointmentRepository.class),users,mock(DoctorProfileService.class));
  assertEquals(403,assertThrows(ResponseStatusException.class,()->service.start("patient@test.invalid",id)).getStatusCode().value());assertEquals(403,assertThrows(ResponseStatusException.class,()->service.complete("patient@test.invalid",id)).getStatusCode().value());assertEquals(TeleconsultationSession.State.WAITING,session.getState());
 }
 @Test void activeSessionRemainsAccessibleAfterTheAppointmentGraceWindow(){var sessions=mock(TeleconsultationSessionRepository.class);var users=mock(UserRepository.class);var doctorUser=new UserAccount("Doc","Tor","doctor-late@test.invalid","hash",UserRole.DOCTOR,"ACTIVE");var doctor=mock(Doctor.class);when(doctor.getUser()).thenReturn(doctorUser);var patientUser=new UserAccount("Pat","Ient","patient-late@test.invalid","hash",UserRole.USER,"ACTIVE");org.springframework.test.util.ReflectionTestUtils.setField(patientUser,"id",UUID.randomUUID());var appointment=new Appointment(doctor,new Patient(patientUser),"Consultation","50",Instant.now().minusSeconds(7200),Instant.now().minusSeconds(3600),null,ConsultationMode.VIDEO);appointment.changeStatus(AppointmentStatus.CONFIRMED,null);var session=new TeleconsultationSession(appointment);session.start();UUID id=UUID.randomUUID();when(users.findByEmailIgnoreCase(patientUser.getEmail())).thenReturn(Optional.of(patientUser));when(sessions.findParticipantContext(id)).thenReturn(Optional.of(session));assertEquals(TeleconsultationSession.State.ACTIVE,new TeleconsultationService(sessions,mock(AppointmentRepository.class),users,mock(DoctorProfileService.class)).get(patientUser.getEmail(),id).state());}
}
