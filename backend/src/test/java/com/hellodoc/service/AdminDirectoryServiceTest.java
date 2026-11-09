package com.hellodoc.service;

import com.hellodoc.dto.AccountStatusRequest;
import com.hellodoc.entity.*;
import com.hellodoc.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminDirectoryServiceTest {
 @Test void listsDoctorProfilesFromDoctorRepository(){
  DoctorRepository doctors=mock(DoctorRepository.class);PatientRepository patients=mock(PatientRepository.class);AdminDirectoryService service=service(doctors,patients);
  Doctor doctor=mock(Doctor.class);UserAccount user=new UserAccount("Maya","Example","maya@example.test","555","Cabinet","hash",UserRole.DOCTOR,"ACTIVE");DoctorApplication application=mock(DoctorApplication.class);
  when(doctor.getUser()).thenReturn(user);when(doctor.getSpecialties()).thenReturn(Set.of(new Specialty("Cardiologie")));when(doctor.getApplication()).thenReturn(application);when(doctor.getServiceTitle()).thenReturn("Dr Maya");when(doctor.getCity()).thenReturn("Tunis");when(doctor.getLocation()).thenReturn("Cabinet");when(doctor.getDescription()).thenReturn("Présentation");when(application.getId()).thenReturn(UUID.randomUUID());when(application.getLicenseNumber()).thenReturn("MED-77");when(doctors.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(new PageImpl<>(List.of(doctor),PageRequest.of(0,50),1));
  var result=service.doctors(0,50);
  assertEquals(1,result.getTotalElements());assertEquals("maya@example.test",result.getContent().getFirst().email());assertEquals("MED-77",result.getContent().getFirst().licenseNumber());verify(doctors).findAll(any(org.springframework.data.domain.Pageable.class));
 }
 @Test void listsPatientProfilesFromPatientRepository(){
  DoctorRepository doctors=mock(DoctorRepository.class);PatientRepository patients=mock(PatientRepository.class);AdminDirectoryService service=service(doctors,patients);
  UserAccount user=new UserAccount("Maya","Example","maya@example.test","555","Home","hash",UserRole.USER,"ACTIVE");Patient patient=new Patient(user);when(patients.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(new PageImpl<>(List.of(patient),PageRequest.of(0,50),1));
  var result=service.patients(0,50);assertEquals("maya@example.test",result.getContent().getFirst().email());assertEquals("ACTIVE",result.getContent().getFirst().status());
 }
 @Test void administratorCanDeactivateAndReactivateDoctorAndPatientProfiles(){
  DoctorRepository doctors=mock(DoctorRepository.class);PatientRepository patients=mock(PatientRepository.class);AdminDirectoryService service=service(doctors,patients);
  Doctor doctor=mock(Doctor.class);UserAccount doctorUser=new UserAccount("D","Example","d@example.test","555","Clinic","hash",UserRole.DOCTOR,"ACTIVE");DoctorApplication application=mock(DoctorApplication.class);
  when(doctors.findById(any())).thenReturn(Optional.of(doctor));when(doctor.getUser()).thenReturn(doctorUser);when(doctor.getApplication()).thenReturn(application);when(doctor.getSpecialties()).thenReturn(Set.of());when(application.getId()).thenReturn(UUID.randomUUID());when(application.getLicenseNumber()).thenReturn("MED-1");when(doctor.getServiceTitle()).thenReturn("Dr D");when(doctor.getCity()).thenReturn("Tunis");when(doctor.getLocation()).thenReturn("Clinic");when(doctor.getDescription()).thenReturn("");
  service.setDoctorStatus(UUID.randomUUID(),new AccountStatusRequest("INACTIVE"));assertEquals("INACTIVE",doctorUser.getStatus());verify(doctor).setActive(false);
  UserAccount patientUser=new UserAccount("P","Example","p@example.test","555","Home","hash",UserRole.USER,"ACTIVE");Patient patient=new Patient(patientUser);when(patients.findById(any())).thenReturn(Optional.of(patient));
  service.setPatientStatus(UUID.randomUUID(),new AccountStatusRequest("INACTIVE"));assertEquals("INACTIVE",patientUser.getStatus());service.setPatientStatus(UUID.randomUUID(),new AccountStatusRequest("ACTIVE"));assertEquals("ACTIVE",patientUser.getStatus());
 }
 @Test void deletingDoctorRemovesItsDependentProfilesAndPrivateDocument(){
  DoctorRepository doctors=mock(DoctorRepository.class);PatientRepository patients=mock(PatientRepository.class);AdminAccountDeletionRepository deletion=mock(AdminAccountDeletionRepository.class);DoctorDocumentStorageService documents=mock(DoctorDocumentStorageService.class);
  AdminDirectoryService service=new AdminDirectoryService(doctors,patients,deletion,documents);UUID doctorId=UUID.randomUUID(),applicationId=UUID.randomUUID(),userId=UUID.randomUUID();Doctor doctor=mock(Doctor.class);DoctorApplication application=mock(DoctorApplication.class);UserAccount user=mock(UserAccount.class);
  when(doctors.findById(doctorId)).thenReturn(Optional.of(doctor));when(doctor.getId()).thenReturn(doctorId);when(doctor.getApplication()).thenReturn(application);when(doctor.getUser()).thenReturn(user);when(application.getId()).thenReturn(applicationId);when(application.getDocumentStorageKey()).thenReturn("private-key.pdf");when(user.getId()).thenReturn(userId);
  org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
  try{service.deleteDoctor(doctorId);org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations().forEach(org.springframework.transaction.support.TransactionSynchronization::afterCommit);}
  finally{org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();}
  org.mockito.InOrder order=inOrder(deletion);order.verify(deletion).deleteAbsences(doctorId);order.verify(deletion).deleteAvailabilities(doctorId);order.verify(deletion).deleteDoctorSpecialties(doctorId);order.verify(deletion).deleteDoctor(doctorId);order.verify(deletion).deleteDoctorApplication(applicationId);order.verify(deletion).deleteUser(userId);verify(documents).delete("private-key.pdf");
 }
 @Test void deletingPatientRemovesPatientProfileAndUser(){
  DoctorRepository doctors=mock(DoctorRepository.class);PatientRepository patients=mock(PatientRepository.class);AdminAccountDeletionRepository deletion=mock(AdminAccountDeletionRepository.class);AdminDirectoryService service=new AdminDirectoryService(doctors,patients,deletion,mock(DoctorDocumentStorageService.class));
  UUID patientId=UUID.randomUUID();Patient patient=mock(Patient.class);UserAccount user=mock(UserAccount.class);when(patients.findById(patientId)).thenReturn(Optional.of(patient));when(patient.getUser()).thenReturn(user);when(user.getRole()).thenReturn(UserRole.USER);
  when(patient.getId()).thenReturn(patientId);when(user.getId()).thenReturn(UUID.randomUUID());service.deletePatient(patientId);org.mockito.InOrder order=inOrder(deletion);order.verify(deletion).deletePatient(patientId);order.verify(deletion).deleteUser(user.getId());
 }
 private AdminDirectoryService service(DoctorRepository doctors,PatientRepository patients){return new AdminDirectoryService(doctors,patients,mock(AdminAccountDeletionRepository.class),mock(DoctorDocumentStorageService.class));}
}

