package com.hellodoc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SpecialtyRequest(@NotBlank(message = "Le nom de la spécialité est obligatoire.") @Size(max = 120, message = "Le nom ne peut pas dépasser 120 caractères.") String name) { }
