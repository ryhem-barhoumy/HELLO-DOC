package com.hellodoc.controller;

import com.hellodoc.dto.*;
import com.hellodoc.service.PrescriptionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api")
public class PrescriptionController {
    private final PrescriptionService service;
    public PrescriptionController(PrescriptionService service){this.service=service;}
    @PostMapping("/appointments/{id}/prescriptions") @PreAuthorize("hasRole('DOCTOR')")
    public PrescriptionDto create(Authentication auth,@PathVariable UUID id,@Valid @RequestBody PrescriptionRequest request){return service.create(auth.getName(),id,request);}
    @GetMapping("/appointments/{id}/prescriptions") @PreAuthorize("hasRole('DOCTOR')")
    public List<PrescriptionDto> doctorList(Authentication auth,@PathVariable UUID id){return service.forAppointmentDoctor(auth.getName(),id);}
    @PutMapping("/prescriptions/{id}/amend") @PreAuthorize("hasRole('DOCTOR')")
    public PrescriptionDto amend(Authentication auth,@PathVariable UUID id,@Valid @RequestBody PrescriptionRequest request){return service.amend(auth.getName(),id,request);}
    @PostMapping("/prescriptions/{id}/finalize") @PreAuthorize("hasRole('DOCTOR')")
    public PrescriptionDto finalizePrescription(Authentication auth,@PathVariable UUID id){return service.finalizePrescription(auth.getName(),id);}
    @GetMapping("/patients/me/prescriptions") @PreAuthorize("hasAnyRole('USER','PATIENT')")
    public List<PrescriptionDto> patientList(Authentication auth){return service.forPatient(auth.getName());}
}
