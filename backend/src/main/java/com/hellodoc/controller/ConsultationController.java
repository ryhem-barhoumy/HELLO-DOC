package com.hellodoc.controller;
import com.hellodoc.dto.*;
import com.hellodoc.service.ConsultationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api") @PreAuthorize("hasRole('DOCTOR')")
public class ConsultationController {
    private final ConsultationService consultations;
    public ConsultationController(ConsultationService consultations){this.consultations=consultations;}
    @GetMapping("/appointments/{id}/consultation-note") public ConsultationNoteDto note(Authentication auth,@PathVariable UUID id){return consultations.note(auth.getName(),id);}
    @PutMapping("/appointments/{id}/consultation-note") public ConsultationNoteDto save(Authentication auth,@PathVariable UUID id,@Valid @RequestBody ConsultationNoteRequest request){return consultations.save(auth.getName(),id,request);}
    @PostMapping("/appointments/{id}/consultation-note/finalize") public ConsultationNoteDto finalizeReport(Authentication auth,@PathVariable UUID id){return consultations.finalizeReport(auth.getName(),id);}
    @GetMapping("/appointments/{id}/consultation-note/history") public List<Map<String,Object>> history(Authentication auth,@PathVariable UUID id){return consultations.history(auth.getName(),id);}
    @GetMapping("/doctors/me/patients") public PagedResponse<DoctorPatientSummary> patients(Authentication auth,@RequestParam(required=false) String q,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return consultations.patients(auth.getName(),q,page,size);}
    @GetMapping("/doctors/me/patients/{id}") public DoctorPatientDossier dossier(Authentication auth,@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return consultations.dossier(auth.getName(),id,page,size);}
    @GetMapping("/appointments/{id}/clinical-context") public DoctorPatientDossier clinicalContext(Authentication auth,@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size){return consultations.clinicalContext(auth.getName(),id,page,size);}
}
