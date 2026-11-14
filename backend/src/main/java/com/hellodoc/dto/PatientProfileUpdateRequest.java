package com.hellodoc.dto;
import jakarta.validation.constraints.*;
public record PatientProfileUpdateRequest(@NotBlank @Size(max=100) String firstName,@NotBlank @Size(max=100) String lastName,@Pattern(regexp="(?:|[0-9]{8,15})",message="Le téléphone doit contenir entre 8 et 15 chiffres.") String phone,@Size(max=300) String address,@NotBlank String currentPassword) { }
