package com.hellodoc.service;

import com.hellodoc.dto.RegistrationRequest;
import com.hellodoc.dto.DoctorRegistrationRequest;
import com.hellodoc.entity.UserAccount;
import com.hellodoc.entity.DoctorApplication;
import com.hellodoc.entity.UserRole;
import com.hellodoc.repository.DoctorApplicationRepository;
import com.hellodoc.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountServiceTest {
 @Test void publicRegistrationCannotChooseAnElevatedRoleAndStoresOnlyBcryptHash(){
  UserRepository users=mock(UserRepository.class); DoctorApplicationRepository applications=mock(DoctorApplicationRepository.class); BCryptPasswordEncoder encoder=new BCryptPasswordEncoder();
  when(users.existsByEmailIgnoreCase("person@example.com")).thenReturn(false);
  when(users.save(any(UserAccount.class))).thenAnswer(invocation->invocation.getArgument(0));
  UserAccount created=new AccountService(users,encoder,applications,new ObjectMapper()).register(new RegistrationRequest("A","Person"," Person@Example.com ",null,null,"safe-password-123",true));
  assertEquals(UserRole.USER,created.getRole()); assertNotEquals("safe-password-123",created.getPasswordHash());
  assertTrue(encoder.matches("safe-password-123",created.getPasswordHash())); assertEquals("person@example.com",created.getEmail());
 }
 @Test void publicDoctorSignupStartsAsOrdinaryPendingUser(){
  UserRepository users=mock(UserRepository.class); DoctorApplicationRepository applications=mock(DoctorApplicationRepository.class); BCryptPasswordEncoder encoder=new BCryptPasswordEncoder();
  when(users.existsByEmailIgnoreCase("doctor@example.com")).thenReturn(false); when(users.save(any(UserAccount.class))).thenAnswer(invocation->invocation.getArgument(0));
  when(applications.save(any(DoctorApplication.class))).thenAnswer(invocation->invocation.getArgument(0));
  var body=new DoctorRegistrationRequest("D","Person","doctor@example.com","555","Clinic","password-123",true,"Cardiology","LIC-1","Dr D",30,"08:00","16:00",java.util.List.of("1"),10,"Clinic address","Tunis","Description",java.util.List.of(new DoctorRegistrationRequest.ServiceInput("Visit","50 DT")));
  var created=new AccountService(users,encoder,applications,new ObjectMapper()).registerDoctor(body);
  assertEquals(UserRole.USER,created.getUser().getRole()); assertEquals("PENDING",created.getStatus()); assertTrue(encoder.matches("password-123",created.getUser().getPasswordHash()));
 }
}
