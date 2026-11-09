package com.hellodoc.dto;
import java.util.UUID;
public record PatientProfileDto(UUID id,String firstName,String lastName,String fullName,String email,String phone,String address) { }
