package com.hellodoc.dto;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;
import com.hellodoc.entity.ConsultationMode;
public record AppointmentCreateRequest(@NotNull UUID doctorId,@NotBlank @Size(max=160) String serviceName,@NotNull Instant startAt,@Size(max=500) String reason,@Size(max=80) String expectedServicePrice,ConsultationMode consultationMode) {
 public AppointmentCreateRequest(UUID doctorId,String serviceName,Instant startAt,String reason){this(doctorId,serviceName,startAt,reason,null,null);}
 public AppointmentCreateRequest(UUID doctorId,String serviceName,Instant startAt,String reason,String expectedServicePrice){this(doctorId,serviceName,startAt,reason,expectedServicePrice,null);}
}
