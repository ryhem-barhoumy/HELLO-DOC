package com.hellodoc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AccountStatusRequest(@NotBlank @Pattern(regexp="ACTIVE|INACTIVE") String status) { }
