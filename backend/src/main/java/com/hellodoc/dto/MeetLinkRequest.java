package com.hellodoc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MeetLinkRequest(@NotBlank @Size(max=500) String url) {}
