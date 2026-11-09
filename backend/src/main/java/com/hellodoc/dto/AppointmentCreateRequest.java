package com.hellodoc.dto;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;
public record AppointmentCreateRequest(@NotNull UUID doctorId,@NotBlank @Size(max=160) String serviceName,@NotNull Instant startAt,@Size(max=500) String reason) { }
