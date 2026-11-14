package com.hellodoc.service;

import com.hellodoc.dto.AbsenceRequest;
import com.hellodoc.entity.*;
import com.hellodoc.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AbsenceServiceTest {
    final DoctorProfileService profiles = mock(DoctorProfileService.class);
    final AbsenceRepository absences = mock(AbsenceRepository.class);
    final AppointmentRepository appointments = mock(AppointmentRepository.class);
    final AppointmentStatusHistoryRepository history = mock(AppointmentStatusHistoryRepository.class);
    final NotificationService notifications = mock(NotificationService.class);
    final AbsenceService service = new AbsenceService(profiles, absences, appointments, history, notifications);
    final UserAccount doctorUser = new UserAccount("Amel", "Doctor", "doctor@example.test", "hash", UserRole.DOCTOR, "ACTIVE");
    final Doctor doctor = new Doctor(doctorUser, null, "Dr Amel", "Tunis", "Cabinet", "Cabinet", "", 30, 0, "[]");
    final LocalDate date = LocalDate.now(ZoneId.of("Africa/Tunis")).plusDays(2);

    Appointment appointment() {
        var patient = new Patient(new UserAccount("Patient", "Test", "patient@example.test", "hash", UserRole.USER, "ACTIVE"));
        Instant start = date.atTime(10, 0).atZone(ZoneId.of("Africa/Tunis")).toInstant();
        return new Appointment(doctor, patient, "Consultation", "50 DT", start, start.plusSeconds(1800), null);
    }
    AbsenceRequest request(boolean confirmed) {
        return new AbsenceRequest(date,date,LocalTime.of(10,0),LocalTime.of(10,30),"Motif privé", "Commentaire privé",confirmed);
    }
    void setup(Appointment appointment) {
        when(profiles.own(anyString())).thenReturn(doctor);
        when(profiles.lockOwnSchedule(anyString())).thenReturn(doctor);
        when(appointments.findOverlapping(any(),anyList(),any(),any())).thenReturn(List.of(appointment));
        when(absences.save(any())).thenAnswer(i -> i.getArgument(0));
    }
    @Test void previewDoesNotMutateOrNotify() {
        Appointment appointment = appointment(); setup(appointment);
        assertEquals(1,service.preview("doctor@example.test",request(false)).get("affectedAppointments"));
        assertEquals(AppointmentStatus.PENDING,appointment.getStatus());
        verify(absences,never()).save(any()); verifyNoInteractions(history,notifications);
    }
    @Test void conflictRequiresExplicitPhysicianConfirmation() {
        Appointment appointment = appointment(); setup(appointment);
        assertEquals(409,assertThrows(ResponseStatusException.class,()->service.create("doctor@example.test",request(false))).getStatusCode().value());
        assertEquals(AppointmentStatus.PENDING,appointment.getStatus());
        verify(absences,never()).save(any()); verifyNoInteractions(history,notifications);
    }
    @Test void confirmedAbsenceRequestsReschedulingWithoutDisclosingPrivateReason() {
        Appointment appointment = appointment(); setup(appointment);
        service.create("doctor@example.test",request(true));
        assertEquals(AppointmentStatus.TO_RESCHEDULE,appointment.getStatus());
        ArgumentCaptor<AppointmentStatusHistory> change = ArgumentCaptor.forClass(AppointmentStatusHistory.class);
        verify(history).save(change.capture());
        assertEquals(AppointmentStatus.PENDING,change.getValue().getOldStatus());
        assertFalse(change.getValue().getReason().contains("privé"));
        verify(notifications).create(eq(appointment.getPatient().getUser()),eq(appointment),eq("APPOINTMENT_TO_RESCHEDULE"),anyString(),argThat(message -> !message.contains("privé") && message.contains("nouveau créneau")));
        appointment.reschedule(appointment.getStartAt().plusSeconds(3600),appointment.getEndAt().plusSeconds(3600),null);
        assertEquals(AppointmentStatus.PENDING,appointment.getStatus());
    }
    @Test void partialAbsenceUsesExactBusinessTimezoneBoundsAndIgnoresStartedVisits() {
        Appointment past = appointment(); past.reschedule(Instant.now().minusSeconds(60),Instant.now().plusSeconds(1200),null); setup(past);
        assertEquals(0,service.preview("doctor@example.test",request(false)).get("affectedAppointments"));
        verify(appointments).findOverlapping(eq(doctor.getId()),anyList(),eq(date.atTime(10,0).atZone(ZoneId.of("Africa/Tunis")).toInstant()),eq(date.atTime(10,30).atZone(ZoneId.of("Africa/Tunis")).toInstant()));
    }
}
