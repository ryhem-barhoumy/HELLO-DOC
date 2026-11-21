package com.hellodoc.dto;
import com.hellodoc.entity.ConsultationNote;
import java.time.*;
import java.util.UUID;
public record ConsultationNoteDto(UUID id,String resume,String observation,String treatment,LocalDate nextCheck,String privateNote,long version,Instant createdAt,Instant updatedAt) {
    public static ConsultationNoteDto from(ConsultationNote n){return new ConsultationNoteDto(n.getId(),n.getResume(),n.getObservation(),n.getTreatment(),n.getNextCheck(),n.getPrivateNote(),n.getVersion(),n.getCreatedAt(),n.getUpdatedAt());}
}
