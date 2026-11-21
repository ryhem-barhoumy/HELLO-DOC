package com.hellodoc.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public record ConsultationNoteRequest(@NotBlank @Size(max=4000) String resume,@Size(max=8000) String observation,@Size(max=8000) String treatment,LocalDate nextCheck,@Size(max=4000) String privateNote,@PositiveOrZero Long version){}
