package com.hellodoc.dto;

import jakarta.validation.constraints.*;

/** Registration establishes identity; practice configuration follows approval. */
public record DoctorRegistrationRequest(
    @NotBlank @Size(min=2,max=100) String firstName,
    @NotBlank @Size(min=2,max=100) String lastName,
    @NotBlank @Email @Pattern(regexp="^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$",message="Saisissez une adresse e-mail valide.") @Size(max=254) String email,
    @NotBlank @Pattern(regexp="[0-9]{8,15}",message="Le téléphone doit contenir entre 8 et 15 chiffres, sans lettres.") String phone,
    @NotBlank @Size(max=300) String address,
    @NotBlank @Size(min=8,max=72) String password,
    @AssertTrue boolean consent,
    @NotBlank @Size(max=120) String specialty,
    @NotBlank @Size(max=100) String licenseNumber,
    @NotBlank @Size(max=120) String city) { }
