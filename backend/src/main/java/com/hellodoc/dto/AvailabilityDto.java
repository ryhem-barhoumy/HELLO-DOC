package com.hellodoc.dto;
import com.hellodoc.entity.Availability; import java.time.LocalTime; import java.util.UUID;
public record AvailabilityDto(UUID id,int isoWeekday,int period,LocalTime startTime,LocalTime endTime,int slotDurationMinutes,int pauseMinutes) { public static AvailabilityDto from(Availability a){return new AvailabilityDto(a.getId(),a.getIsoWeekday(),a.getWindowNumber(),a.getStartTime(),a.getEndTime(),a.getSlotDurationMinutes(),a.getPauseMinutes());} }
