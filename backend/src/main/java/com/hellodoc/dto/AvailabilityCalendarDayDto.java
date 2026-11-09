package com.hellodoc.dto;

import java.time.LocalDate;
import java.time.Instant;

public record AvailabilityCalendarDayDto(LocalDate date, String status, int availableSlots,
                                         Instant firstAvailableAt) { }
