package com.hellodoc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellodoc.dto.TimeSlotDto;
import com.hellodoc.entity.*;
import com.hellodoc.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AppointmentServiceTest {
    @Test void freeSlotsExcludeEveryOverlappingActiveAppointment(){
        AppointmentRepository repository=mock(AppointmentRepository.class);DoctorProfileService profiles=mock(DoctorProfileService.class);UUID doctorId=UUID.randomUUID();
        Instant ten=Instant.parse("2026-11-10T09:00:00Z"),tenThirty=ten.plusSeconds(1800),eleven=ten.plusSeconds(3600);
        when(profiles.slots(doctorId,LocalDate.parse("2026-11-10"))).thenReturn(List.of(new TimeSlotDto(ten.toString(),ten,ten.plusSeconds(1800),"AVAILABLE"),new TimeSlotDto(tenThirty.toString(),tenThirty,eleven,"AVAILABLE"),new TimeSlotDto(eleven.toString(),eleven,eleven.plusSeconds(1800),"AVAILABLE")));
        Appointment occupied=mock(Appointment.class);when(occupied.getId()).thenReturn(UUID.randomUUID());when(occupied.getStartAt()).thenReturn(ten.plusSeconds(900));when(occupied.getEndAt()).thenReturn(ten.plusSeconds(2700));
        when(repository.findOverlapping(eq(doctorId),anyList(),any(),any())).thenReturn(List.of(occupied));
        AppointmentService service=new AppointmentService(repository,mock(AppointmentStatusHistoryRepository.class),mock(ReviewRepository.class),mock(DoctorRepository.class),mock(PatientRepository.class),mock(UserRepository.class),profiles,mock(NotificationService.class),new ObjectMapper());
        var free=service.freeSlots(doctorId,LocalDate.parse("2026-11-10"));
        assertEquals(List.of(eleven.toString()),free.stream().map(TimeSlotDto::id).toList());
    }

    @Test void appointmentDetailsAreRestrictedToTheAuthenticatedParticipant(){
        AppointmentRepository repository=mock(AppointmentRepository.class);UserRepository users=mock(UserRepository.class);String email="patient@example.test";UserAccount user=new UserAccount("Pat","Example",email,"hash",UserRole.USER,"ACTIVE");
        when(users.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));when(repository.findOwned(any(),eq(email))).thenReturn(Optional.empty());
        AppointmentService service=new AppointmentService(repository,mock(AppointmentStatusHistoryRepository.class),mock(ReviewRepository.class),mock(DoctorRepository.class),mock(PatientRepository.class),users,mock(DoctorProfileService.class),mock(NotificationService.class),new ObjectMapper());
        assertEquals(404,assertThrows(ResponseStatusException.class,()->service.get(email,UUID.randomUUID())).getStatusCode().value());
    }

    @Test void pendingDoctorCannotBeBooked(){
        AppointmentRepository repository=mock(AppointmentRepository.class);UserRepository users=mock(UserRepository.class);DoctorRepository doctors=mock(DoctorRepository.class);String email="patient@example.test";UserAccount user=new UserAccount("Pat","Example",email,"hash",UserRole.USER,"ACTIVE");UUID doctorId=UUID.randomUUID();Doctor doctor=mock(Doctor.class);UserAccount doctorUser=new UserAccount("Dr","Pending","doctor@example.test","hash",UserRole.DOCTOR,"PENDING");
        when(users.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));when(doctors.lockActiveById(doctorId)).thenReturn(Optional.of(doctor));when(doctor.getUser()).thenReturn(doctorUser);
        AppointmentService service=new AppointmentService(repository,mock(AppointmentStatusHistoryRepository.class),mock(ReviewRepository.class),doctors,mock(PatientRepository.class),users,mock(DoctorProfileService.class),mock(NotificationService.class),new ObjectMapper());
        var request=new com.hellodoc.dto.AppointmentCreateRequest(doctorId,"Consultation",Instant.now(),null);
        assertEquals(404,assertThrows(ResponseStatusException.class,()->service.book(email,request)).getStatusCode().value());verifyNoInteractions(repository);
    }
    @Test void bookingRejectsAChangedDisplayedPriceBeforeReserving(){
        AppointmentRepository repository=mock(AppointmentRepository.class);UserRepository users=mock(UserRepository.class);DoctorRepository doctors=mock(DoctorRepository.class);DoctorProfileService profiles=mock(DoctorProfileService.class);
        String email="patient@example.test";UUID doctorId=UUID.randomUUID();UserAccount patient=new UserAccount("Pat","Test",email,"hash",UserRole.USER,"ACTIVE");
        Doctor doctor=mock(Doctor.class);when(doctor.getUser()).thenReturn(new UserAccount("Dr","Test","doctor@example.test","hash",UserRole.DOCTOR,"ACTIVE"));
        when(users.findByEmailIgnoreCase(email)).thenReturn(Optional.of(patient));when(doctors.lockActiveById(doctorId)).thenReturn(Optional.of(doctor));
        when(profiles.servicesFor(doctor)).thenReturn(List.of(new com.hellodoc.dto.MedicalServiceDto("Consultation","65 DT")));
        AppointmentService service=new AppointmentService(repository,mock(AppointmentStatusHistoryRepository.class),mock(ReviewRepository.class),doctors,mock(PatientRepository.class),users,profiles,mock(NotificationService.class),new ObjectMapper());
        var request=new com.hellodoc.dto.AppointmentCreateRequest(doctorId,"Consultation",Instant.now().plusSeconds(86400),null,"50 DT");
        assertEquals(409,assertThrows(ResponseStatusException.class,()->service.book(email,request)).getStatusCode().value());verifyNoInteractions(repository);
    }

    @Test void confirmedVideoAppointmentCanCloseAfterExplicitMeetCompletionWithoutWebRtcSession(){
        String email="doctor@example.test";UserAccount doctorUser=new UserAccount("Dr","Test",email,"hash",UserRole.DOCTOR,"ACTIVE");Doctor doctor=new Doctor(doctorUser,null,"Generaliste","Tunis","Adresse","Cabinet","Description",30,0,"[]");Patient patient=new Patient(new UserAccount("Pat","Test","patient@example.test","hash",UserRole.USER,"ACTIVE"));Instant start=Instant.now().plusSeconds(3600);Appointment appointment=new Appointment(doctor,patient,"Consultation","50",start,start.plusSeconds(1800),null,ConsultationMode.VIDEO);appointment.changeStatus(AppointmentStatus.CONFIRMED,null);appointment.setMeetLink("https://meet.google.com/abc-defg-hij");appointment.markMeetCallEnded(email);UUID id=UUID.randomUUID();org.springframework.test.util.ReflectionTestUtils.setField(appointment,"id",id);
        AppointmentRepository appointments=mock(AppointmentRepository.class);UserRepository users=mock(UserRepository.class);AppointmentStatusHistoryRepository history=mock(AppointmentStatusHistoryRepository.class);ReviewRepository reviews=mock(ReviewRepository.class);DoctorProfileService profiles=mock(DoctorProfileService.class);TeleconsultationSessionRepository sessions=mock(TeleconsultationSessionRepository.class);
        when(users.findByEmailIgnoreCase(email)).thenReturn(Optional.of(doctorUser));when(profiles.own(email)).thenReturn(doctor);when(appointments.findOwned(id,email)).thenReturn(Optional.of(appointment));when(sessions.findByAppointmentId(id)).thenReturn(Optional.empty());when(history.findByAppointmentIdOrderByChangedAtAsc(id)).thenReturn(List.of());when(reviews.findByAppointmentId(id)).thenReturn(Optional.empty());
        AppointmentService service=new AppointmentService(appointments,history,reviews,mock(DoctorRepository.class),mock(PatientRepository.class),users,profiles,mock(NotificationService.class),new ObjectMapper(),sessions);
        var result=service.changeStatus(email,id,new com.hellodoc.dto.AppointmentStatusRequest(AppointmentStatus.COMPLETED,"Clôture après appel Meet"));assertEquals(AppointmentStatus.COMPLETED,result.status());assertNotNull(appointment.getMeetCallEndedAt());verify(sessions).findByAppointmentId(id);
    }

    @Test void videoAppointmentWithoutCompletedWebRtcOrMeetCannotClose(){
        String email="doctor@example.test";UserAccount doctorUser=new UserAccount("Dr","Test",email,"hash",UserRole.DOCTOR,"ACTIVE");Doctor doctor=new Doctor(doctorUser,null,"Generaliste","Tunis","Adresse","Cabinet","Description",30,0,"[]");Patient patient=new Patient(new UserAccount("Pat","Test","patient@example.test","hash",UserRole.USER,"ACTIVE"));Instant start=Instant.now().minusSeconds(3600);Appointment appointment=new Appointment(doctor,patient,"Consultation","50",start,start.plusSeconds(1800),null,ConsultationMode.VIDEO);appointment.changeStatus(AppointmentStatus.CONFIRMED,null);UUID id=UUID.randomUUID();org.springframework.test.util.ReflectionTestUtils.setField(appointment,"id",id);
        AppointmentRepository appointments=mock(AppointmentRepository.class);UserRepository users=mock(UserRepository.class);TeleconsultationSessionRepository sessions=mock(TeleconsultationSessionRepository.class);when(users.findByEmailIgnoreCase(email)).thenReturn(Optional.of(doctorUser));when(appointments.findOwned(id,email)).thenReturn(Optional.of(appointment));when(sessions.findByAppointmentId(id)).thenReturn(Optional.empty());
        AppointmentService service=new AppointmentService(appointments,mock(AppointmentStatusHistoryRepository.class),mock(ReviewRepository.class),mock(DoctorRepository.class),mock(PatientRepository.class),users,mock(DoctorProfileService.class),mock(NotificationService.class),new ObjectMapper(),sessions);
        assertEquals(409,assertThrows(ResponseStatusException.class,()->service.changeStatus(email,id,new com.hellodoc.dto.AppointmentStatusRequest(AppointmentStatus.COMPLETED,null))).getStatusCode().value());assertEquals(AppointmentStatus.CONFIRMED,appointment.getStatus());
    }

}
