package com.hellodoc.controller;

import com.hellodoc.dto.SessionUser;
import com.hellodoc.dto.DoctorApplicationSummary;
import com.hellodoc.dto.DecisionRequest;
import com.hellodoc.dto.AccountStatusRequest;
import com.hellodoc.entity.UserRole;
import com.hellodoc.repository.DoctorApplicationRepository;
import com.hellodoc.repository.UserRepository;
import com.hellodoc.service.DoctorApprovalService;
import com.hellodoc.service.AdminDirectoryService;
import com.hellodoc.service.AdminProfileService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
 private final UserRepository users; private final DoctorApplicationRepository applications; private final DoctorApprovalService approvals; private final AdminDirectoryService directory; private final AdminProfileService profiles;
 public AdminController(UserRepository users,DoctorApplicationRepository applications,DoctorApprovalService approvals,AdminDirectoryService directory,AdminProfileService profiles){this.users=users;this.applications=applications;this.approvals=approvals;this.directory=directory;this.profiles=profiles;}
 @GetMapping("/me") public com.hellodoc.dto.PatientProfileDto me(Authentication auth){return profiles.get(auth.getName());}
 @PutMapping("/me") public com.hellodoc.dto.PatientProfileDto update(Authentication auth,@Valid @RequestBody com.hellodoc.dto.PatientProfileUpdateRequest request){return profiles.update(auth.getName(),request);}
 @GetMapping("/dashboard") public Map<String,Object> dashboard(){
  return Map.of("asOf",Instant.now().toString(),"patientsCount",users.countByRoleAndStatus(UserRole.USER,"ACTIVE")+users.countByRole(UserRole.PATIENT),
   "doctorsCount",users.countByRole(UserRole.DOCTOR),"pendingDoctorApprovals",applications.countByStatus("PENDING"));
 }
 @GetMapping("/doctors") public List<DoctorApplicationSummary> doctors(@RequestParam(defaultValue="PENDING") String status){return applications.findByStatusOrderByIdDesc(status).stream().map(DoctorApplicationSummary::from).toList();}
 @GetMapping("/doctors/directory") public com.hellodoc.dto.PagedResponse<com.hellodoc.dto.AdminDoctorDto> doctorDirectory(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size){return com.hellodoc.dto.PagedResponse.from(directory.doctors(page,size));}
 @GetMapping("/doctors/directory/{id}") public com.hellodoc.dto.AdminDoctorDto doctorDetails(@PathVariable UUID id){return directory.doctor(id);}
 @GetMapping("/patients") public com.hellodoc.dto.PagedResponse<com.hellodoc.dto.AdminPatientDto> patients(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size){return com.hellodoc.dto.PagedResponse.from(directory.patients(page,size));}
 @PatchMapping("/doctors/{id}/status") public com.hellodoc.dto.AdminDoctorDto doctorStatus(@PathVariable UUID id,@Valid @RequestBody AccountStatusRequest request){return directory.setDoctorStatus(id,request);}
 @PatchMapping("/patients/{id}/status") public com.hellodoc.dto.AdminPatientDto patientStatus(@PathVariable UUID id,@Valid @RequestBody AccountStatusRequest request){return directory.setPatientStatus(id,request);}
 @DeleteMapping("/doctors/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void deleteDoctor(@PathVariable UUID id){directory.deleteDoctor(id);}
 @DeleteMapping("/patients/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void deletePatient(@PathVariable UUID id){directory.deletePatient(id);}
 @PostMapping("/doctors/{id}/approve") public DoctorApplicationSummary approve(@PathVariable UUID id,@RequestBody(required=false) DecisionRequest request){return approvals.approve(id,request);}
 @PostMapping("/doctors/{id}/reject") public DoctorApplicationSummary reject(@PathVariable UUID id,@Valid @RequestBody DecisionRequest request){return approvals.reject(id,request);}
}
