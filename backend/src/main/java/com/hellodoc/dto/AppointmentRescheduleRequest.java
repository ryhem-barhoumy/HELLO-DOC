package com.hellodoc.dto;
import jakarta.validation.constraints.*;
import java.time.Instant;
public record AppointmentRescheduleRequest(@NotNull Instant startAt,@Size(max=500) String reason) { }
