package com.hellodoc.service;

import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DoctorApprovalEmailSenderTest {
 @Test void googleAppPasswordPresentationSpacesAreRemovedWithoutChangingOtherProviders(){
  var gmail=new org.springframework.mail.javamail.JavaMailSenderImpl();gmail.setPassword("abcd efgh ijkl mnop");
  new DoctorApprovalEmailSender(gmail,"test@example.test","http://localhost:4200","smtp.gmail.com");
  assertEquals("abcdefghijklmnop",gmail.getPassword());
  var other=new org.springframework.mail.javamail.JavaMailSenderImpl();other.setPassword("abcd efgh ijkl mnop");
  new DoctorApprovalEmailSender(other,"test@example.test","http://localhost:4200","smtp.other.test");
  assertEquals("abcd efgh ijkl mnop",other.getPassword());
 }
 @Test void sendsProfessionalApprovalMessageWithoutIncludingPassword(){
  JavaMailSender sender=mock(JavaMailSender.class);
  DoctorApprovalEmailSender emails=new DoctorApprovalEmailSender(sender,"no-reply@hellodoc.tn","http://localhost:4200/","smtp.example.test");
  emails.send(new DoctorApplicationApprovedEvent(java.util.UUID.randomUUID(),"doctor@example.test","Sami","Ben Ali"));
  ArgumentCaptor<SimpleMailMessage> captor=ArgumentCaptor.forClass(SimpleMailMessage.class);
  verify(sender).send(captor.capture());
  SimpleMailMessage message=captor.getValue();
  assertArrayEquals(new String[]{"doctor@example.test"},message.getTo());
  assertEquals("Votre inscription médecin Hello Doc est approuvée",message.getSubject());
  assertTrue(message.getText().contains("Bonjour Docteur Sami"));
  assertTrue(message.getText().contains("Votre compte professionnel est maintenant activé"));
  assertTrue(message.getText().contains("http://localhost:4200/connexion"));
  assertFalse(message.getText().contains("password"));
 }
}
