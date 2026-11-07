package com.hellodoc.controller;

import com.hellodoc.dto.SessionUser;
import com.hellodoc.dto.DoctorApplicationSummary;
import com.hellodoc.dto.DecisionRequest;
import com.hellodoc.entity.DoctorApplication;
import com.hellodoc.entity.UserAccount;
import com.hellodoc.entity.UserRole;
import com.hellodoc.repository.DoctorApplicationRepository;
import com.hellodoc.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
 private final UserRepository users; private final DoctorApplicationRepository applications;
 public AdminController(UserRepository users,DoctorApplicationRepository applications){this.users=users;this.applications=applications;}
 @GetMapping("/me") public SessionUser me(Authentication auth){return SessionUser.from(users.findByEmailIgnoreCase(auth.getName()).orElseThrow());}
 @GetMapping("/dashboard") public Map<String,Object> dashboard(){
  return Map.of("asOf",Instant.now().toString(),"patientsCount",users.countByRoleAndStatus(UserRole.USER,"ACTIVE")+users.countByRole(UserRole.PATIENT),
   "doctorsCount",users.countByRole(UserRole.DOCTOR),"pendingDoctorApprovals",applications.countByStatus("PENDING"));
 }
 @GetMapping("/doctors") public List<DoctorApplicationSummary> doctors(@RequestParam(defaultValue="PENDING") String status){return applications.findByStatusOrderByIdDesc(status).stream().map(DoctorApplicationSummary::from).toList();}
 @PostMapping("/doctors/{id}/approve") @Transactional public DoctorApplicationSummary approve(@PathVariable UUID id,@RequestBody(required=false) DecisionRequest request){
  DoctorApplication application=pending(id); UserAccount user=application.getUser(); application.decide("APPROVED",request==null?null:request.reason()); user.setRoleAndStatus(UserRole.DOCTOR,"APPROVED"); users.save(user); return DoctorApplicationSummary.from(application);
 }
 @PostMapping("/doctors/{id}/reject") @Transactional public DoctorApplicationSummary reject(@PathVariable UUID id,@Valid @RequestBody DecisionRequest request){
  if(request.reason()==null||request.reason().isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A reason is required to reject a doctor application.");
  DoctorApplication application=pending(id); application.decide("REJECTED",request.reason().trim()); application.getUser().setRoleAndStatus(UserRole.USER,"REJECTED"); return DoctorApplicationSummary.from(application);
 }
 private DoctorApplication pending(UUID id){DoctorApplication a=applications.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));if(!"PENDING".equals(a.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"This application has already been decided.");return a;}
}
