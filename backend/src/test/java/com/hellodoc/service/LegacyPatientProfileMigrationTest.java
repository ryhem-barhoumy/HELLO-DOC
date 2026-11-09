package com.hellodoc.service;

import com.hellodoc.entity.*;
import com.hellodoc.repository.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LegacyPatientProfileMigrationTest {
 @Test void createsProfilesForLegacyUsersButSkipsDoctorsAndExistingPatients(){
  UserRepository users=mock(UserRepository.class);PatientRepository patients=mock(PatientRepository.class);DoctorApplicationRepository applications=mock(DoctorApplicationRepository.class);
  UserAccount legacy=mock(UserAccount.class);UserAccount doctorApplicant=mock(UserAccount.class);java.util.UUID legacyId=java.util.UUID.randomUUID(),doctorId=java.util.UUID.randomUUID();when(legacy.getId()).thenReturn(legacyId);when(doctorApplicant.getId()).thenReturn(doctorId);
  when(users.findAllByRoleIn(List.of(UserRole.USER,UserRole.PATIENT))).thenReturn(List.of(legacy,doctorApplicant));when(patients.existsByUserId(legacyId)).thenReturn(false);when(applications.existsByUser_Id(legacyId)).thenReturn(false);when(patients.existsByUserId(doctorId)).thenReturn(false);when(applications.existsByUser_Id(doctorId)).thenReturn(true);
  int created=new LegacyPatientProfileMigration(users,patients,applications).createMissingProfiles();
  assertEquals(1,created);verify(patients).save(argThat(patient->patient.getUser()==legacy));verify(patients,never()).save(argThat(patient->patient.getUser()==doctorApplicant));
 }
}
