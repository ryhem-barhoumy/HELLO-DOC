package com.hellodoc.dto;
import jakarta.validation.constraints.*;
public record ReviewCreateRequest(@Min(1) @Max(5) int rating,@NotBlank @Pattern(regexp="YES|NO") String recommend,@Size(max=2000) String comment) { }
