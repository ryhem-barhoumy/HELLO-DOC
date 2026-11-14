package com.hellodoc.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank @Email @jakarta.validation.constraints.Pattern(regexp="^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") String email, @NotBlank String password) { }
