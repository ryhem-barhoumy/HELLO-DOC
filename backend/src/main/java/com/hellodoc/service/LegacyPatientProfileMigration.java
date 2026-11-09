package com.hellodoc.service;

import com.hellodoc.entity.Patient;
import com.hellodoc.entity.UserRole;
import com.hellodoc.repository.DoctorApplicationRepository;
import com.hellodoc.repository.PatientRepository;
import com.hellodoc.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class LegacyPatientProfileMigration {
    private final UserRepository users;
    private final PatientRepository patients;
    private final DoctorApplicationRepository applications;
    public LegacyPatientProfileMigration(UserRepository users,PatientRepository patients,DoctorApplicationRepository applications){this.users=users;this.patients=patients;this.applications=applications;}

    @Transactional
    public int createMissingProfiles(){
        int created=0;
        for(var user:users.findAllByRoleIn(List.of(UserRole.USER,UserRole.PATIENT))){
            if(patients.existsByUserId(user.getId())||applications.existsByUser_Id(user.getId()))continue;
            patients.save(new Patient(user));created++;
        }
        return created;
    }
}
