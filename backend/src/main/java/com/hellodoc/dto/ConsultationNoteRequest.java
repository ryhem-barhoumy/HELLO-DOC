package com.hellodoc.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public record ConsultationNoteRequest(@Size(max=4000) String resume,@Size(max=8000) String observation,@Size(max=8000) String treatment,LocalDate nextCheck,@Size(max=4000) String privateNote,@PositiveOrZero Long version,@Size(max=120) String recommendedDelay,@Size(max=2000) String followUpInstructions,@Size(max=500) String correctionReason){
    public ConsultationNoteRequest(String resume,String observation,String treatment,LocalDate nextCheck,String privateNote,Long version){this(resume,observation,treatment,nextCheck,privateNote,version,null,null,null);}
    public ConsultationNoteRequest(String resume,String observation,String treatment,LocalDate nextCheck,String privateNote,Long version,String recommendedDelay,String followUpInstructions){this(resume,observation,treatment,nextCheck,privateNote,version,recommendedDelay,followUpInstructions,null);}
}
