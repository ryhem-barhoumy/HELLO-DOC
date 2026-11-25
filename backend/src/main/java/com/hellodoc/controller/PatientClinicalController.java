package com.hellodoc.controller;

import com.hellodoc.dto.PatientConsultationDto;
import com.hellodoc.service.PatientClinicalService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/patients/me") @PreAuthorize("hasAnyRole('USER','PATIENT')")
public class PatientClinicalController {
    private final PatientClinicalService service;
    public PatientClinicalController(PatientClinicalService service){this.service=service;}
    @GetMapping("/consultations") public List<PatientConsultationDto> consultations(Authentication auth){return service.myConsultations(auth.getName());}
}
