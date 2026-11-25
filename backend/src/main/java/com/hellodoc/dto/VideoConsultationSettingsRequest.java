package com.hellodoc.dto;

import jakarta.validation.constraints.NotNull;

public record VideoConsultationSettingsRequest(@NotNull Boolean enabled) { }
