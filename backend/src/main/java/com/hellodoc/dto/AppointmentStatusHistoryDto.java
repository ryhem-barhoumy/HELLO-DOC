package com.hellodoc.dto;
import com.hellodoc.entity.AppointmentStatus;
import java.time.Instant;
public record AppointmentStatusHistoryDto(AppointmentStatus oldStatus,AppointmentStatus newStatus,Instant changedAt,String actorName,String reason) { }
