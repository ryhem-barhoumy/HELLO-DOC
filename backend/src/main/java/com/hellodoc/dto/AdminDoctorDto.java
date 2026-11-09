package com.hellodoc.dto;

import com.hellodoc.entity.Doctor;
import java.util.UUID;

public record AdminDoctorDto(UUID id,String applicationId,String name,String fullName,String serviceTitle,String email,String phone,
        String address,String specialty,String licenseNumber,String city,String location,String description,String status,boolean hasDocument) {
    public static AdminDoctorDto from(Doctor doctor) {
        var user=doctor.getUser();
        String fullName=user.getFirstName()+" "+user.getLastName();
        String specialty=doctor.getSpecialties().stream().map(s->s.getName()).sorted().findFirst().orElse("");
        return new AdminDoctorDto(doctor.getId(),doctor.getApplication().getId().toString(),fullName,fullName,doctor.getServiceTitle(),user.getEmail(),user.getPhone(),
                user.getAddress(),specialty,doctor.getApplication().getLicenseNumber(),doctor.getCity(),doctor.getLocation(),
                doctor.getDescription(),user.getStatus(),doctor.getApplication().hasDoctorDocument());
    }
}
