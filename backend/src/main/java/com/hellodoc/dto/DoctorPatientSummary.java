package com.hellodoc.dto;
import java.time.Instant;
import java.util.UUID;
public record DoctorPatientSummary(UUID id,String fullName,String phone,String email,long completedCount,Instant lastVisit){}
