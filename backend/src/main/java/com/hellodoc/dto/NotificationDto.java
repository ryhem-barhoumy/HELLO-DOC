package com.hellodoc.dto;
import com.hellodoc.entity.Notification;
import java.time.Instant;
import java.util.UUID;
public record NotificationDto(UUID id,String type,String title,String message,Instant createdAt,Instant readAt,UUID appointmentId) { public static NotificationDto from(Notification n){return new NotificationDto(n.getId(),n.getType(),n.getTitle(),n.getMessage(),n.getCreatedAt(),n.getReadAt(),n.getAppointment()==null?null:n.getAppointment().getId());} }
