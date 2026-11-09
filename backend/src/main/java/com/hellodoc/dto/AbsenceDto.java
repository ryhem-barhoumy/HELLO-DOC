package com.hellodoc.dto;
import com.hellodoc.entity.Absence; import java.time.LocalDate; import java.time.LocalTime; import java.util.UUID;
public record AbsenceDto(UUID id,LocalDate startDate,LocalDate endDate,LocalTime startTime,LocalTime endTime,String reason,String comment) { public static AbsenceDto from(Absence a){return new AbsenceDto(a.getId(),a.getStartDate(),a.getEndDate(),a.getStartTime(),a.getEndTime(),a.getReason(),a.getComment());} }
