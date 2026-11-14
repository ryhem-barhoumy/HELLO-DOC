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
import java.time.Duration;
import java.time.LocalTime;

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

   // No working hours or services are invented during registration.
   DoctorApplication application=new DoctorApplication(user,"Dr "+request.firstName().trim()+" "+request.lastName().trim(),request.specialty().trim(),request.licenseNumber().trim(),request.city().trim(),request.address().trim(),30,"","",0,"[]","[]","");
   application.attachDoctorDocument(stored.storageKey(),stored.originalName(),stored.mediaType(),stored.sizeBytes());
   DoctorApplication saved=applications.save(application);
   if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)documents.delete(stored.storageKey());}});
   return saved;
  } catch(RuntimeException ex){documents.delete(stored.storageKey());throw ex;}
 }
}
