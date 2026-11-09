package com.hellodoc.dto;
import java.time.Instant;
public record TimeSlotDto(String id,Instant startsAt,Instant endsAt,String status,String periodLabel) {
 public TimeSlotDto(String id,Instant startsAt,Instant endsAt,String status){this(id,startsAt,endsAt,status,"Première plage");}
}
