package com.hellodoc.dto;
import jakarta.validation.constraints.*;
public record AvailabilityRequest(@Min(1) @Max(7) int isoWeekday,@NotNull String startTime,@NotNull String endTime,@Min(5) @Max(240) int slotDurationMinutes,@Min(0) @Max(240) int pauseMinutes) { }
