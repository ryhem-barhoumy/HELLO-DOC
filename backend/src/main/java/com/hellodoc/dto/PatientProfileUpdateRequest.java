package com.hellodoc.dto;
import jakarta.validation.constraints.*;
public record PatientProfileUpdateRequest(@NotBlank @Size(max=100) String firstName,@NotBlank @Size(max=100) String lastName,@Size(max=32) String phone,@Size(max=300) String address,@NotBlank String currentPassword) { }
