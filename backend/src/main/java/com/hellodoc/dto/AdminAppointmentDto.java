package com.hellodoc.dto;
import com.hellodoc.entity.Appointment;
import java.time.Instant;
import java.util.UUID;
/** Operational metadata only: no clinical note, patient reason or private physician information. */
public record AdminAppointmentDto(UUID id,Instant startsAt,Instant endsAt,String status,String serviceName,String servicePrice,String patientName,String doctorName,String location) {
 public static AdminAppointmentDto from(Appointment a){var p=a.getPatient().getUser();var d=a.getDoctor();var u=d.getUser();return new AdminAppointmentDto(a.getId(),a.getStartAt(),a.getEndAt(),a.getStatus().name(),a.getServiceName(),a.getServicePrice(),p.getFirstName()+" "+p.getLastName(),u.getFirstName()+" "+u.getLastName(),d.getLocation());}
}
