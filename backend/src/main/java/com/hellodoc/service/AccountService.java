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
import com.hellodoc.service.PatientProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.UUID;

@Service
public class AccountService {
 private final UserRepository users; private final PasswordEncoder passwords; private final DoctorApplicationRepository applications; private final ObjectMapper json; private final PatientProfileService patientProfiles; private final DoctorDocumentStorageService documents;
 public AccountService(UserRepository users,PasswordEncoder passwords,DoctorApplicationRepository applications,ObjectMapper json,PatientProfileService patientProfiles,DoctorDocumentStorageService documents){this.users=users;this.passwords=passwords;this.applications=applications;this.json=json;this.patientProfiles=patientProfiles;this.documents=documents;}
 @org.springframework.transaction.annotation.Transactional
 public UserAccount register(RegistrationRequest request){
  String email=request.email().trim().toLowerCase();
  if(users.existsByEmailIgnoreCase(email))throw new ResponseStatusException(HttpStatus.CONFLICT,"An account with this email already exists.");
  // Role is deliberately assigned here; registration payloads cannot select privileges.
  UserAccount user=users.save(new UserAccount(request.firstName().trim(),request.lastName().trim(),email,request.phone(),request.address(),passwords.encode(request.password()),UserRole.USER,"ACTIVE"));patientProfiles.createForRegistration(user);return user;
 }
 @Transactional public DoctorApplication registerDoctor(DoctorRegistrationRequest request,MultipartFile document){
  String email=request.email().trim().toLowerCase();
  if(users.existsByEmailIgnoreCase(email))throw new ResponseStatusException(HttpStatus.CONFLICT,"An account with this email already exists.");
  DoctorDocumentStorageService.StoredDocument stored=documents.store(document);
  try {
   UserAccount user=users.save(new UserAccount(request.firstName().trim(),request.lastName().trim(),email,request.phone(),request.address(),passwords.encode(request.password()),UserRole.USER,"PENDING"));
   String days=json.writeValueAsString(request.workingDays()); String services=json.writeValueAsString(request.services());
     DoctorApplication application=new DoctorApplication(user,request.serviceTitle().trim(),request.specialty().trim(),request.licenseNumber().trim(),request.city().trim(),request.location().trim(),request.durationMinutes(),request.workStart(),request.workEnd(),request.pauseMinutes(),days,services,request.description().trim());
     application.setSecondWorkWindow(request.secondWorkStart(),request.secondWorkEnd());
   application.attachDoctorDocument(stored.storageKey(),stored.originalName(),stored.mediaType(),stored.sizeBytes());
   DoctorApplication saved=applications.save(application);
   if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)documents.delete(stored.storageKey());}});
   return saved;
  } catch(JsonProcessingException|RuntimeException ex){documents.delete(stored.storageKey());if(ex instanceof RuntimeException runtime)throw runtime;throw new IllegalStateException("Could not store doctor registration details",ex);}
 }
}
