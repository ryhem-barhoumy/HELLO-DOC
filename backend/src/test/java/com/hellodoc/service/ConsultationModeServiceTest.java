package com.hellodoc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellodoc.dto.AppointmentCreateRequest;
import com.hellodoc.dto.AppointmentStatusRequest;
import com.hellodoc.dto.MedicalServiceDto;
import com.hellodoc.dto.TimeSlotDto;
import com.hellodoc.entity.*;
import com.hellodoc.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ConsultationModeServiceTest {
    private final ZoneId zone=ZoneId.of("Africa/Tunis");

    @Test
    void legacyBookingWithoutModeDefaultsToInPersonAndDoesNotCreateVideoSession(){
        var setup=bookingSetup(false);
        AppointmentCreateRequest legacy=new AppointmentCreateRequest(setup.doctorId,"Consultation",setup.slotStart,"Reason");
        setup.service.book(setup.patientUser.getEmail(),legacy);
        var saved=captureSaved(setup.appointments);
        assertEquals(ConsultationMode.IN_PERSON,saved.getConsultationMode());
        verifyNoInteractions(setup.sessions);
    }

    @Test
    void patientCanChooseVideoOnlyWhenDoctorEnabledIt(){
        var setup=bookingSetup(true);
        setup.service.book(setup.patientUser.getEmail(),new AppointmentCreateRequest(setup.doctorId,"Consultation",setup.slotStart,"Reason","50",ConsultationMode.VIDEO));
        assertEquals(ConsultationMode.VIDEO,captureSaved(setup.appointments).getConsultationMode());

        var disabled=bookingSetup(false);
        var error=assertThrows(ResponseStatusException.class,()->disabled.service.book(disabled.patientUser.getEmail(),new AppointmentCreateRequest(disabled.doctorId,"Consultation",disabled.slotStart,"Reason","50",ConsultationMode.VIDEO)));
        assertEquals(409,error.getStatusCode().value());
        verify(disabled.appointments,never()).saveAndFlush(any());
    }

    @Test
    void videoAppointmentCannotBeCompletedUntilItsVideoSessionIsCompleted(){
        var appointment=confirmedAppointment(ConsultationMode.VIDEO);
        var setup=statusSetup(appointment);
        when(setup.sessions.findByAppointmentId(appointment.getId())).thenReturn(Optional.of(new TeleconsultationSession(appointment)));
        var waiting=assertThrows(ResponseStatusException.class,()->setup.service.changeStatus("doctor@example.test",appointment.getId(),new AppointmentStatusRequest(AppointmentStatus.COMPLETED,null)));
        assertEquals(409,waiting.getStatusCode().value());
        assertEquals(AppointmentStatus.CONFIRMED,appointment.getStatus());

        var finished=new TeleconsultationSession(appointment);finished.start();finished.complete();
        when(setup.sessions.findByAppointmentId(appointment.getId())).thenReturn(Optional.of(finished));
        setup.service.changeStatus("doctor@example.test",appointment.getId(),new AppointmentStatusRequest(AppointmentStatus.COMPLETED,null));
        assertEquals(AppointmentStatus.COMPLETED,appointment.getStatus());
    }

    @Test
    void physicalAppointmentCanBeCompletedWithoutAnyTeleconsultationSession(){
        var appointment=confirmedAppointment(ConsultationMode.IN_PERSON);var setup=statusSetup(appointment);
        setup.service.changeStatus("doctor@example.test",appointment.getId(),new AppointmentStatusRequest(AppointmentStatus.COMPLETED,null));
        assertEquals(AppointmentStatus.COMPLETED,appointment.getStatus());
        verifyNoInteractions(setup.sessions);
    }

    private BookingSetup bookingSetup(boolean videoEnabled){
        var apps=mock(AppointmentRepository.class);var sessions=mock(TeleconsultationSessionRepository.class);var histories=mock(AppointmentStatusHistoryRepository.class);var reviews=mock(ReviewRepository.class);var doctors=mock(DoctorRepository.class);var patients=mock(PatientRepository.class);var users=mock(UserRepository.class);var profiles=mock(DoctorProfileService.class);var notifications=mock(NotificationService.class);
        UUID doctorId=UUID.randomUUID();String email="patient@example.test";var patientUser=new UserAccount("Pat","Ient",email,"hash",UserRole.USER,"ACTIVE");var patient=new Patient(patientUser);var doctorUser=new UserAccount("Doc","Tor","doctor@example.test","hash",UserRole.DOCTOR,"ACTIVE");var doctor=mock(Doctor.class);when(doctor.getId()).thenReturn(doctorId);when(doctor.getUser()).thenReturn(doctorUser);when(doctor.isVideoConsultationEnabled()).thenReturn(videoEnabled);when(doctors.lockActiveById(doctorId)).thenReturn(Optional.of(doctor));when(users.findByEmailIgnoreCase(email)).thenReturn(Optional.of(patientUser));when(patients.findByUserId(patientUser.getId())).thenReturn(Optional.of(patient));when(profiles.servicesFor(doctor)).thenReturn(List.of(new MedicalServiceDto("Consultation","50")));
        LocalDate date=LocalDate.now(zone).plusDays(4);Instant start=date.atTime(10,0).atZone(zone).toInstant();var slot=new TimeSlotDto(start.toString(),start,start.plusSeconds(1800),"AVAILABLE");when(profiles.slots(doctorId,date)).thenReturn(List.of(slot));when(apps.findOverlapping(eq(doctorId),anyList(),any(),any())).thenReturn(List.of());when(apps.saveAndFlush(any())).thenAnswer(i->i.getArgument(0));when(histories.findByAppointmentIdOrderByChangedAtAsc(any())).thenReturn(List.of());
        var service=new AppointmentService(apps,histories,reviews,doctors,patients,users,profiles,notifications,new ObjectMapper(),sessions);
        return new BookingSetup(service,apps,sessions,doctorId,patientUser,start);
    }

    private Appointment captureSaved(AppointmentRepository repository){var captor=org.mockito.ArgumentCaptor.forClass(Appointment.class);verify(repository).saveAndFlush(captor.capture());return captor.getValue();}

    private Appointment confirmedAppointment(ConsultationMode mode){var doctorUser=new UserAccount("Doc","Tor","doctor@example.test","hash",UserRole.DOCTOR,"ACTIVE");var doctor=mock(Doctor.class);when(doctor.getUser()).thenReturn(doctorUser);var patientUser=new UserAccount("Pat","Ient","patient@example.test","hash",UserRole.USER,"ACTIVE");var appointment=new Appointment(doctor,new Patient(patientUser),"Consultation","50",Instant.now().minusSeconds(7200),Instant.now().minusSeconds(3600),null,mode);appointment.changeStatus(AppointmentStatus.CONFIRMED,null);org.springframework.test.util.ReflectionTestUtils.setField(appointment,"id",UUID.randomUUID());return appointment;}

    private StatusSetup statusSetup(Appointment appointment){var apps=mock(AppointmentRepository.class);var histories=mock(AppointmentStatusHistoryRepository.class);var reviews=mock(ReviewRepository.class);var doctors=mock(DoctorRepository.class);var patients=mock(PatientRepository.class);var users=mock(UserRepository.class);var profiles=mock(DoctorProfileService.class);var notifications=mock(NotificationService.class);var sessions=mock(TeleconsultationSessionRepository.class);var actor=new UserAccount("Doc","Tor","doctor@example.test","hash",UserRole.DOCTOR,"ACTIVE");when(users.findByEmailIgnoreCase(actor.getEmail())).thenReturn(Optional.of(actor));when(profiles.own(actor.getEmail())).thenReturn(appointment.getDoctor());when(apps.findOwned(appointment.getId(),actor.getEmail())).thenReturn(Optional.of(appointment));when(histories.findByAppointmentIdOrderByChangedAtAsc(appointment.getId())).thenReturn(List.of());when(reviews.findByAppointmentId(appointment.getId())).thenReturn(Optional.empty());var service=new AppointmentService(apps,histories,reviews,doctors,patients,users,profiles,notifications,new ObjectMapper(),sessions);return new StatusSetup(service,sessions);}

    private record BookingSetup(AppointmentService service,AppointmentRepository appointments,TeleconsultationSessionRepository sessions,UUID doctorId,UserAccount patientUser,Instant slotStart){}
    private record StatusSetup(AppointmentService service,TeleconsultationSessionRepository sessions){}
}
