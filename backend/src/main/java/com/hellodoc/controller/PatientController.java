package com.hellodoc.controller;
import com.hellodoc.dto.*; import com.hellodoc.service.PatientProfileService; import com.hellodoc.service.AppointmentService; import jakarta.validation.Valid; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/patients/me") @PreAuthorize("hasAnyRole('USER','PATIENT')")
public class PatientController {
 private final PatientProfileService patients; private final AppointmentService appointments; public PatientController(PatientProfileService patients,AppointmentService appointments){this.patients=patients;this.appointments=appointments;}
 @GetMapping public PatientProfileDto me(Authentication auth){return patients.get(auth.getName());}
 @GetMapping("/dashboard") public PatientDashboardDto dashboard(Authentication auth){return appointments.patientDashboard(auth.getName());}
 @PutMapping public PatientProfileDto update(Authentication auth,@Valid @RequestBody PatientProfileUpdateRequest request){return patients.update(auth.getName(),request);}
}
