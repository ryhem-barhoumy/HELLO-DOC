package com.hellodoc.dto;

import jakarta.validation.constraints.Size;

public record DecisionRequest(@Size(max=1000) String reason) { }
