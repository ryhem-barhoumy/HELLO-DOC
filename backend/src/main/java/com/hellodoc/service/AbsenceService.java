package com.hellodoc.service;

import com.hellodoc.dto.*;
import com.hellodoc.entity.*;
import com.hellodoc.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AbsenceService {
    private static final ZoneId ZONE = ZoneId.of("Africa/Tunis");
    private static final List<AppointmentStatus> RESERVED = List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);
    private final DoctorProfileService profiles;
    private final AbsenceRepository absences;
    private final AppointmentRepository appointments;
    private final AppointmentStatusHistoryRepository history;
    private final NotificationService notifications;

    public AbsenceService(DoctorProfileService profiles, AbsenceRepository absences,
                          AppointmentRepository appointments, AppointmentStatusHistoryRepository history,
                          NotificationService notifications) {
        this.profiles = profiles; this.absences = absences; this.appointments = appointments;
        this.history = history; this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public List<AbsenceDto> list(String email) {
        return absences.findByDoctorIdOrderByStartDateAsc(profiles.own(email).getId()).stream().map(AbsenceDto::from).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Integer> preview(String email, AbsenceRequest request) {
        Doctor doctor = profiles.own(email);
        validated(doctor, request);
        return Map.of("affectedAppointments", affected(doctor, request).size());
    }

    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public AbsenceDto create(String email, AbsenceRequest request) {
        Doctor doctor = profiles.lockOwnSchedule(email);
        Absence absence = validated(doctor, request);
        requestRescheduling(doctor, request);
        return AbsenceDto.from(absences.save(absence));
    }

    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public AbsenceDto update(String email, UUID id, AbsenceRequest request) {
        Doctor doctor = profiles.lockOwnSchedule(email);
        Absence absence = absences.findByIdAndDoctorId(id, doctor.getId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Absence introuvable."));
        validated(doctor, request);
        requestRescheduling(doctor, request);
        absence.update(request.startDate(), request.endDate(), request.startTime(), request.endTime(),
                request.reason().trim(), clean(request.comment()));
        return AbsenceDto.from(absence);
    }

    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void delete(String email, UUID id) {
        Doctor doctor = profiles.lockOwnSchedule(email);
        Absence absence = absences.findByIdAndDoctorId(id, doctor.getId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Absence introuvable."));
        // Removing an absence must not silently restore a patient's former reservation.
        absences.delete(absence);
    }

    private Absence validated(Doctor doctor, AbsenceRequest request) {
        return new Absence(doctor, request.startDate(), request.endDate(), request.startTime(),
                request.endTime(), request.reason().trim(), clean(request.comment()));
    }

    private List<Appointment> affected(Doctor doctor, AbsenceRequest request) {
        Instant from = request.startTime() == null ? request.startDate().atStartOfDay(ZONE).toInstant()
                : request.startDate().atTime(request.startTime()).atZone(ZONE).toInstant();
        Instant to = request.endTime() == null ? request.endDate().plusDays(1).atStartOfDay(ZONE).toInstant()
                : request.endDate().atTime(request.endTime()).atZone(ZONE).toInstant();
        Instant now = Instant.now();
        return appointments.findOverlapping(doctor.getId(), RESERVED, from, to).stream()
                .filter(a -> a.getStartAt().isAfter(now)).toList();
    }

    private void requestRescheduling(Doctor doctor, AbsenceRequest request) {
        List<Appointment> affected = affected(doctor, request);
        if (!affected.isEmpty() && !Boolean.TRUE.equals(request.confirmRescheduling())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    affected.size() + " rendez-vous sont concernés. Confirmez la demande de reprogrammation ou choisissez une autre période.");
        }
        for (Appointment appointment : affected) {
            AppointmentStatus before = appointment.getStatus();
            appointment.changeStatus(AppointmentStatus.TO_RESCHEDULE, null);
            String reason = "Indisponibilité du médecin : choisissez un nouveau créneau.";
            history.save(new AppointmentStatusHistory(appointment, doctor.getUser(), before, AppointmentStatus.TO_RESCHEDULE, reason));
            String date = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm").withZone(ZONE).format(appointment.getStartAt());
            notifications.create(appointment.getPatient().getUser(), appointment, "APPOINTMENT_TO_RESCHEDULE",
                    "Votre rendez-vous est à reprogrammer",
                    "Votre médecin est indisponible pour le rendez-vous du " + date
                            + ". Choisissez un nouveau créneau depuis vos rendez-vous. Aucune nouvelle date ne vous est imposée.");
        }
    }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
