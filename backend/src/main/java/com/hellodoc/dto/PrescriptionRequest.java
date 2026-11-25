package com.hellodoc.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record PrescriptionRequest(@Size(max=4000) String recommendations, @NotEmpty @Size(max=50) List<@Valid Item> items) {
    public record Item(@NotBlank @Size(max=300) String medication, @NotBlank @Size(max=1000) String instructions) {}
}
