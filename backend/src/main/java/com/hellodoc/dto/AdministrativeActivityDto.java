package com.hellodoc.dto;
import com.hellodoc.entity.AdministrativeActivity;
import java.time.Instant;
import java.util.UUID;
public record AdministrativeActivityDto(UUID id,String actorName,String action,String description,String targetType,UUID targetId,Instant occurredAt) {
 public static AdministrativeActivityDto from(AdministrativeActivity a){String label=switch(a.getAction()){case "DOCTOR_APPROVED"->"Inscription médecin approuvée";case "DOCTOR_REJECTED"->"Inscription médecin refusée";case "ACCOUNT_ACTIVATED"->"Compte activé";case "ACCOUNT_DEACTIVATED"->"Compte désactivé";case "ACCOUNT_DELETED"->"Compte sans historique supprimé";default->"Action administrative";};return new AdministrativeActivityDto(a.getId(),a.getActorName(),a.getAction(),label,a.getTargetType(),a.getTargetId(),a.getOccurredAt());}
}
