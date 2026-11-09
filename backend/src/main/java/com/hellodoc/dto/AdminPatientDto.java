package com.hellodoc.dto;

import com.hellodoc.entity.Patient;
import java.util.UUID;

public record AdminPatientDto(UUID id,String name,String fullName,String email,String phone,String address,String status) {
    public static AdminPatientDto from(Patient patient) {
        var user=patient.getUser();
        String fullName=user.getFirstName()+" "+user.getLastName();
        return new AdminPatientDto(patient.getId(),fullName,fullName,user.getEmail(),user.getPhone(),user.getAddress(),user.getStatus());
    }
}
