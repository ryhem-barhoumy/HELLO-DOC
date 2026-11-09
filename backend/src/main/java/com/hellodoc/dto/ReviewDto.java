package com.hellodoc.dto;
import com.hellodoc.entity.Review;
import java.time.Instant;
import java.util.UUID;
public record ReviewDto(UUID id,UUID appointmentId,UUID doctorId,String doctorName,String patientName,int rating,String recommend,String comment,Instant createdAt,Instant updatedAt,String status) { public static ReviewDto from(Review r){return new ReviewDto(r.getId(),r.getAppointment().getId(),r.getDoctor().getId(),r.getDoctor().getUser().getFirstName()+" "+r.getDoctor().getUser().getLastName(),r.getPatient().getUser().getFirstName()+" "+r.getPatient().getUser().getLastName(),r.getRating(),r.getRecommend(),r.getComment(),r.getCreatedAt(),r.getUpdatedAt(),"REVIEW");} }
