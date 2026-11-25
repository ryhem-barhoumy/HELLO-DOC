package com.hellodoc.controller;
import com.hellodoc.dto.TeleconsultationMessageDto;
import com.hellodoc.service.TeleconsultationChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/teleconsultations/{id}/messages") @PreAuthorize("hasAnyRole('DOCTOR','USER','PATIENT')")
public class TeleconsultationChatController{
 public record MessageRequest(@NotBlank @Size(max=2000) String body){}
 private final TeleconsultationChatService service; public TeleconsultationChatController(TeleconsultationChatService service){this.service=service;}
 @GetMapping public List<TeleconsultationMessageDto> history(Authentication a,@PathVariable UUID id){return service.history(a.getName(),id);}
 @PostMapping public TeleconsultationMessageDto send(Authentication a,@PathVariable UUID id,@Valid @RequestBody MessageRequest body){return service.send(a.getName(),id,body.body());}
}
