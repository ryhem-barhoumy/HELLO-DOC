package com.hellodoc.dto;
import com.hellodoc.entity.TeleconsultationMessage;
import java.time.Instant;
import java.util.UUID;
public record TeleconsultationMessageDto(UUID id, String senderName, String body, Instant sentAt){
 public static TeleconsultationMessageDto from(TeleconsultationMessage m){return new TeleconsultationMessageDto(m.getId(),m.getSenderName(),m.getBody(),m.getSentAt());}
}
