package com.hellodoc.service;
import com.hellodoc.dto.*; import com.hellodoc.entity.*; import com.hellodoc.repository.*; import org.springframework.http.HttpStatus; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import org.springframework.web.server.ResponseStatusException;
@Service public class PatientProfileService {
 private final UserRepository users; private final PatientRepository patients; private final PasswordEncoder passwords;
 public PatientProfileService(UserRepository users,PatientRepository patients,PasswordEncoder passwords){this.users=users;this.patients=patients;this.passwords=passwords;}
 @Transactional public PatientProfileDto get(String email){UserAccount u=patientUser(email);if(!patients.existsByUserId(u.getId()))patients.save(new Patient(u));return dto(u);}
 @Transactional public PatientProfileDto update(String email,PatientProfileUpdateRequest r){UserAccount u=patientUser(email);if(!passwords.matches(r.currentPassword(),u.getPasswordHash()))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Le mot de passe actuel est incorrect.");u.updateContactProfile(r.firstName().trim(),r.lastName().trim(),clean(r.phone()),clean(r.address()));if(!patients.existsByUserId(u.getId()))patients.save(new Patient(u));return dto(u);}
 @Transactional public void createForRegistration(UserAccount user){if(user.getRole()==UserRole.USER&&!patients.existsByUserId(user.getId()))patients.save(new Patient(user));}
 private UserAccount patientUser(String email){UserAccount u=users.findByEmailIgnoreCase(email).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));if((u.getRole()!=UserRole.USER&&u.getRole()!=UserRole.PATIENT)||!"ACTIVE".equals(u.getStatus()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Un compte patient actif est requis.");return u;}
 private PatientProfileDto dto(UserAccount u){return new PatientProfileDto(u.getId(),u.getFirstName(),u.getLastName(),u.getFirstName()+" "+u.getLastName(),u.getEmail(),u.getPhone(),u.getAddress());}
 private String clean(String s){return s==null||s.isBlank()?null:s.trim();}
}
