package com.hellodoc.dto;

import com.hellodoc.entity.DoctorApplication;
import java.util.UUID;

public record DoctorApplicationSummary(UUID id,String firstName,String lastName,String email,String phone,String name,String fullName,
 String serviceTitle,String specialty,String licenseNumber,String city,String location,String address,String contactAddress,String status,String description,boolean hasDocument) {
 public static DoctorApplicationSummary from(DoctorApplication a){var u=a.getUser();String full=u.getFirstName()+" "+u.getLastName();return new DoctorApplicationSummary(a.getId(),u.getFirstName(),u.getLastName(),u.getEmail(),u.getPhone(),full,full,a.getServiceTitle(),a.getSpecialty(),a.getLicenseNumber(),a.getCity(),a.getPracticeAddress(),a.getPracticeAddress(),u.getAddress(),a.getStatus(),a.getDescription(),a.hasDoctorDocument());}
}
