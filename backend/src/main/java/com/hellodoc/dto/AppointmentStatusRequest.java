package com.hellodoc.dto;
import jakarta.validation.constraints.*;
import com.hellodoc.entity.AppointmentStatus;
public record AppointmentStatusRequest(@NotNull AppointmentStatus status,@Size(max=500) String reason) { }
