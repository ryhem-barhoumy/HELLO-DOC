package com.hellodoc.dto;

import jakarta.validation.constraints.*;

public record RegistrationRequest(@NotBlank @Size(max=100) String firstName, @NotBlank @Size(max=100) String lastName,
 @NotBlank @Email @Size(max=254) String email, @Size(max=32) String phone, @Size(max=300) String address,
 @NotBlank @Size(min=8,max=72) String password, @AssertTrue boolean consent) { }
