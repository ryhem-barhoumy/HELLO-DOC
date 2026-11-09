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
}
