package com.hellodoc.dto;

import com.hellodoc.entity.Appointment;
import com.hellodoc.entity.AppointmentStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AppointmentDto(UUID id,Instant startsAt,Instant endsAt,AppointmentStatus status,String serviceName,String servicePrice,String patientName,String doctorName,String location,String reason,String cancellationReason,List<AppointmentStatusHistoryDto> statusHistory,ReviewDto review) {
    public static AppointmentDto from(Appointment a,List<AppointmentStatusHistoryDto> history,ReviewDto review){var p=a.getPatient().getUser();var d=a.getDoctor();var u=d.getUser();return new AppointmentDto(a.getId(),a.getStartAt(),a.getEndAt(),a.getStatus(),a.getServiceName(),a.getServicePrice(),p.getFirstName()+" "+p.getLastName(),u.getFirstName()+" "+u.getLastName(),d.getLocation(),a.getReason(),a.getCancellationReason(),history,review);}
}
