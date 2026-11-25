package com.hellodoc.dto;
import com.hellodoc.entity.TeleconsultationSession;
import java.time.Instant;
import java.util.UUID;
public record TeleconsultationDto(UUID id, UUID appointmentId, TeleconsultationSession.State state, Instant createdAt, Instant startedAt, Instant completedAt,Instant appointmentStartsAt) {
    public static TeleconsultationDto from(TeleconsultationSession s){return new TeleconsultationDto(s.getId(),s.getAppointment().getId(),s.getState(),s.getCreatedAt(),s.getStartedAt(),s.getCompletedAt(),s.getAppointment().getStartAt());}
}
