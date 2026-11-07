package com.hellodoc.service;

import com.hellodoc.dto.RegistrationRequest;
import com.hellodoc.dto.DoctorRegistrationRequest;
import com.hellodoc.entity.DoctorApplication;
import com.hellodoc.repository.DoctorApplicationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import com.hellodoc.entity.UserAccount;
import com.hellodoc.entity.UserRole;
import com.hellodoc.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {
 private final UserRepository users; private final PasswordEncoder passwords; private final DoctorApplicationRepository applications; private final ObjectMapper json;
 public AccountService(UserRepository users,PasswordEncoder passwords,DoctorApplicationRepository applications,ObjectMapper json){this.users=users;this.passwords=passwords;this.applications=applications;this.json=json;}
 public UserAccount register(RegistrationRequest request){
  String email=request.email().trim().toLowerCase();
  if(users.existsByEmailIgnoreCase(email))throw new ResponseStatusException(HttpStatus.CONFLICT,"An account with this email already exists.");
  // Role is deliberately assigned here; registration payloads cannot select privileges.
  return users.save(new UserAccount(request.firstName().trim(),request.lastName().trim(),email,request.phone(),request.address(),passwords.encode(request.password()),UserRole.USER,"ACTIVE"));
 }
 @Transactional public DoctorApplication registerDoctor(DoctorRegistrationRequest request){
  String email=request.email().trim().toLowerCase();
  if(users.existsByEmailIgnoreCase(email))throw new ResponseStatusException(HttpStatus.CONFLICT,"An account with this email already exists.");
  try {
   UserAccount user=users.save(new UserAccount(request.firstName().trim(),request.lastName().trim(),email,request.phone(),request.address(),passwords.encode(request.password()),UserRole.USER,"PENDING"));
   String days=json.writeValueAsString(request.workingDays()); String services=json.writeValueAsString(request.services());
   return applications.save(new DoctorApplication(user,request.serviceTitle().trim(),request.specialty().trim(),request.licenseNumber().trim(),request.city().trim(),request.location().trim(),request.durationMinutes(),request.workStart(),request.workEnd(),request.pauseMinutes(),days,services,request.description().trim()));
  } catch(JsonProcessingException ex){throw new IllegalStateException("Could not store doctor registration details",ex);}
 }
}
