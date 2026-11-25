package com.hellodoc.dto;
import java.util.*;
public record DoctorPatientDossier(UUID id,String fullName,String email,String phone,long completedCount,List<Visit> appointments,int page,int totalPages) {
    public record Visit(UUID id,java.time.Instant startsAt,java.time.Instant endsAt,String serviceName,String status,ConsultationNoteDto consultationNote,java.util.List<PrescriptionDto> prescriptions){}
}
