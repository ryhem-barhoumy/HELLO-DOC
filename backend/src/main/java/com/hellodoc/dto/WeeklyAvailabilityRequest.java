package com.hellodoc.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record WeeklyAvailabilityRequest(@Min(5) @Max(240) int slotDurationMinutes,
                                        @Min(0) @Max(240) int pauseMinutes,
                                        @NotNull @Size(max=14) List<@Valid Window> windows) {
    public record Window(@Min(1) @Max(7) int isoWeekday,@NotNull String startTime,@NotNull String endTime) { }
}
