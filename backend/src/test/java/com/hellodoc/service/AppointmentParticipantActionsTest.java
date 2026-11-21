package com.hellodoc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellodoc.dto.*;
import com.hellodoc.entity.*;
import com.hellodoc.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppointmentParticipantActionsTest {
    static final String DOCTOR="doctor@example.test", PATIENT="patient@example.test";
    static class Fixture {
        final AppointmentRepository appointments=mock(AppointmentRepository.class);
        final AppointmentStatusHistoryRepository history=mock(AppointmentStatusHistoryRepository.class);
        final ReviewRepository reviews=mock(ReviewRepository.class);
        final DoctorRepository doctors=mock(DoctorRepository.class);
        final UserRepository users=mock(UserRepository.class);
        final DoctorProfileService profiles=mock(DoctorProfileService.class);
        final NotificationService notifications=mock(NotificationService.class);
        final UserAccount doctorUser=new UserAccount("Doctor","Test",DOCTOR,"hash",UserRole.DOCTOR,"ACTIVE");
        final UserAccount patientUser=new UserAccount("Patient","Test",PATIENT,"hash",UserRole.USER,"ACTIVE");
        final UUID id=UUID.randomUUID(),doctorId=UUID.randomUUID();
        final Doctor doctor=mock(Doctor.class);
        final Appointment appointment;
        final AppointmentService service;
        Fixture(AppointmentStatus status){
            when(doctor.getId()).thenReturn(doctorId);when(doctor.getUser()).thenReturn(doctorUser);when(doctor.getLocation()).thenReturn("Cabinet");
            Instant start=Instant.now().plusSeconds(172800);
            appointment=new Appointment(doctor,new Patient(patientUser),"Consultation","50 DT",start,start.plusSeconds(1800),null);
            org.springframework.test.util.ReflectionTestUtils.setField(appointment,"id",id);appointment.changeStatus(status,null);
            when(users.findByEmailIgnoreCase(DOCTOR)).thenReturn(Optional.of(doctorUser));when(users.findByEmailIgnoreCase(PATIENT)).thenReturn(Optional.of(patientUser));
            when(appointments.findOwned(id,DOCTOR)).thenReturn(Optional.of(appointment));when(appointments.findOwned(id,PATIENT)).thenReturn(Optional.of(appointment));
            when(profiles.own(DOCTOR)).thenReturn(doctor);
            when(history.findByAppointmentIdOrderByChangedAtAsc(id)).thenReturn(List.of());when(reviews.findByAppointmentId(id)).thenReturn(Optional.empty());
            service=new AppointmentService(appointments,history,reviews,doctors,mock(PatientRepository.class),users,profiles,notifications,new ObjectMapper());
        }
    }
    @Test void doctorCannotReprogramOrReadPatientRescheduleOptions(){
        var f=new Fixture(AppointmentStatus.PENDING);
        assertEquals(403,assertThrows(ResponseStatusException.class,()->f.service.rescheduleOptions(DOCTOR,f.id,LocalDate.now().plusDays(2))).getStatusCode().value());
        assertEquals(403,assertThrows(ResponseStatusException.class,()->f.service.reschedule(DOCTOR,f.id,new AppointmentRescheduleRequest(Instant.now().plusSeconds(86400),null))).getStatusCode().value());
        verifyNoInteractions(f.appointments,f.history,f.notifications,f.doctors,f.profiles);
    }
    @Test void doctorRefusesPendingRequestInsteadOfCancellingIt(){
        var f=new Fixture(AppointmentStatus.PENDING);
        assertEquals(409,assertThrows(ResponseStatusException.class,()->f.service.cancel(DOCTOR,f.id,"Indisponible")).getStatusCode().value());
        assertEquals(AppointmentStatus.PENDING,f.appointment.getStatus());verifyNoInteractions(f.history,f.notifications);
        assertEquals(AppointmentStatus.REJECTED,f.service.changeStatus(DOCTOR,f.id,new AppointmentStatusRequest(AppointmentStatus.REJECTED,"Indisponible")).status());
        verify(f.notifications).create(eq(f.patientUser),eq(f.appointment),eq("APPOINTMENT_REJECTED"),anyString(),anyString());
    }
    @Test void doctorCanCancelAConfirmedFutureAppointment(){
        var f=new Fixture(AppointmentStatus.CONFIRMED);
        assertEquals(AppointmentStatus.CANCELLED,f.service.cancel(DOCTOR,f.id,"Cabinet fermé").status());
        verify(f.notifications).create(eq(f.patientUser),eq(f.appointment),eq("APPOINTMENT_CANCELLED"),anyString(),anyString());
    }
    @Test void doctorCannotCancelAnAppointmentWaitingForPatientRescheduling(){
        var f=new Fixture(AppointmentStatus.TO_RESCHEDULE);
        assertThrows(ResponseStatusException.class,()->f.service.cancel(DOCTOR,f.id,"Indisponible"));
        assertEquals(AppointmentStatus.TO_RESCHEDULE,f.appointment.getStatus());verifyNoInteractions(f.history,f.notifications);
    }
    @Test void patientCanStillCancelPendingOrToRescheduleAppointment(){
        for(var status:List.of(AppointmentStatus.PENDING,AppointmentStatus.TO_RESCHEDULE)){
            var f=new Fixture(status);assertEquals(AppointmentStatus.CANCELLED,f.service.cancel(PATIENT,f.id,"Empêchement").status());
            verify(f.notifications).create(eq(f.doctorUser),eq(f.appointment),eq("APPOINTMENT_CANCELLED"),anyString(),anyString());
        }
    }
    @Test void patientChoosesNewSlotAfterAbsenceAndDoctorReceivesNewPendingRequest(){
        var f=new Fixture(AppointmentStatus.TO_RESCHEDULE);
        Instant start=Instant.now().plusSeconds(259200);var slot=new TimeSlotDto("slot-id",start,start.plusSeconds(1800),"AVAILABLE","Première plage");
        LocalDate date=start.atZone(ZoneId.of("Africa/Tunis")).toLocalDate();
        when(f.doctors.lockActiveById(f.doctorId)).thenReturn(Optional.of(f.doctor));
        when(f.profiles.slots(f.doctorId,date)).thenReturn(List.of(slot));when(f.appointments.findOverlapping(eq(f.doctorId),anyList(),any(),any())).thenReturn(List.of());
        assertEquals(1,f.service.rescheduleOptions(PATIENT,f.id,date).size());
        var result=f.service.reschedule(PATIENT,f.id,new AppointmentRescheduleRequest(start,"Nouvelle date choisie par le patient"));
        assertEquals(start,result.startsAt());assertEquals(AppointmentStatus.PENDING,result.status());
        verify(f.notifications).create(eq(f.doctorUser),eq(f.appointment),eq("APPOINTMENT_RESCHEDULED"),anyString(),anyString());
    }
    @Test void unrelatedPatientCannotReprogramAnotherPatientsAppointment(){
        var f=new Fixture(AppointmentStatus.PENDING);when(f.appointments.findOwned(f.id,PATIENT)).thenReturn(Optional.empty());
        assertEquals(404,assertThrows(ResponseStatusException.class,()->f.service.reschedule(PATIENT,f.id,new AppointmentRescheduleRequest(Instant.now().plusSeconds(86400),null))).getStatusCode().value());
        verifyNoInteractions(f.doctors,f.profiles,f.history,f.notifications);
    }
    @Test void movingConfirmedAppointmentRequiresANewDoctorsDecision(){
        var f=new Fixture(AppointmentStatus.CONFIRMED);Instant start=Instant.now().plusSeconds(259200);
        when(f.doctors.lockActiveById(f.doctorId)).thenReturn(Optional.of(f.doctor));
        when(f.profiles.slots(eq(f.doctorId),any())).thenReturn(List.of(new TimeSlotDto("new-slot",start,start.plusSeconds(1800),"AVAILABLE","Première plage")));
        when(f.appointments.findOverlapping(eq(f.doctorId),anyList(),any(),any())).thenReturn(List.of());
        assertEquals(AppointmentStatus.PENDING,f.service.reschedule(PATIENT,f.id,new AppointmentRescheduleRequest(start,null)).status());
    }
    @Test void patientCannotMoveAnAlreadyStartedConfirmedAppointment(){
        var f=new Fixture(AppointmentStatus.CONFIRMED);org.springframework.test.util.ReflectionTestUtils.setField(f.appointment,"startAt",Instant.now().minusSeconds(60));
        assertEquals(409,assertThrows(ResponseStatusException.class,()->f.service.rescheduleOptions(PATIENT,f.id,LocalDate.now().plusDays(1))).getStatusCode().value());
        verifyNoInteractions(f.doctors,f.notifications);
    }

}
