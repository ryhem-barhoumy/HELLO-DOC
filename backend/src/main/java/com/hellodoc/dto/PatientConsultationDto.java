package com.hellodoc.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Patient facing clinical data. This DTO deliberately has no private-note or revision fields. */
public record PatientConsultationDto(UUID appointmentId, UUID doctorId, Instant startsAt, String status,
                                     String serviceName, String doctorName,
                                     SharedNote note, List<PrescriptionDto> prescriptions) {
    public record SharedNote(String resume, String observation, String treatment,
                             LocalDate nextCheck, Instant updatedAt,String recommendedDelay,String followUpInstructions) {}
}
