package com.hellodoc.dto;
import jakarta.validation.constraints.*; import java.time.LocalDate; import java.time.LocalTime;
public record AbsenceRequest(@NotNull LocalDate startDate,@NotNull LocalDate endDate,LocalTime startTime,LocalTime endTime,@NotBlank @Size(max=120) String reason,@Size(max=500) String comment,Boolean confirmRescheduling) { }
