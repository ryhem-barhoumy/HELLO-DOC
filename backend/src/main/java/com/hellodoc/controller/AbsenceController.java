package com.hellodoc.controller;
import com.hellodoc.dto.*; import com.hellodoc.service.AbsenceService; import jakarta.validation.Valid; import org.springframework.http.HttpStatus; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/doctors/me/absences") @PreAuthorize("hasRole('DOCTOR')") public class AbsenceController {
 private final AbsenceService absences; public AbsenceController(AbsenceService absences){this.absences=absences;}
 @GetMapping public List<AbsenceDto> list(Authentication auth){return absences.list(auth.getName());}
 @PostMapping("/preview") public Map<String,Integer> preview(Authentication auth,@Valid @RequestBody AbsenceRequest request){return absences.preview(auth.getName(),request);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public AbsenceDto create(Authentication auth,@Valid @RequestBody AbsenceRequest request){return absences.create(auth.getName(),request);}
 @PutMapping("/{id}") public AbsenceDto update(Authentication auth,@PathVariable UUID id,@Valid @RequestBody AbsenceRequest request){return absences.update(auth.getName(),id,request);}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(Authentication auth,@PathVariable UUID id){absences.delete(auth.getName(),id);}
}
