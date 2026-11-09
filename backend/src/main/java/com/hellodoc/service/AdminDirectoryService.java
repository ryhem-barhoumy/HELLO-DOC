package com.hellodoc.service;

import com.hellodoc.dto.*;
import com.hellodoc.entity.*;
import com.hellodoc.repository.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.UUID;

@Service
public class AdminDirectoryService {
    private final DoctorRepository doctors;
    private final PatientRepository patients;
    private final AdminAccountDeletionRepository deletion;
    private final DoctorDocumentStorageService documents;
    public AdminDirectoryService(DoctorRepository doctors,PatientRepository patients,AdminAccountDeletionRepository deletion,DoctorDocumentStorageService documents){this.doctors=doctors;this.patients=patients;this.deletion=deletion;this.documents=documents;}

    @Transactional(readOnly=true)
    public Page<AdminDoctorDto> doctors(int page,int size){return doctors.findAll(pageRequest(page,size,"serviceTitle")).map(AdminDoctorDto::from);}

    @Transactional(readOnly=true)
    public AdminDoctorDto doctor(UUID id){return doctors.findById(id).map(AdminDoctorDto::from).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Médecin introuvable."));}

    @Transactional(readOnly=true)
    public Page<AdminPatientDto> patients(int page,int size){return patients.findAll(pageRequest(page,size,"user.lastName")).map(AdminPatientDto::from);}

    @Transactional
    public AdminDoctorDto setDoctorStatus(UUID id,AccountStatusRequest request){
        Doctor doctor=doctors.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Médecin introuvable."));
        boolean active="ACTIVE".equals(request.status());doctor.setActive(active);doctor.getUser().setStatus(request.status());return AdminDoctorDto.from(doctor);
    }

    @Transactional
    public AdminPatientDto setPatientStatus(UUID id,AccountStatusRequest request){
        Patient patient=patients.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Patient introuvable."));
        UserAccount user=patient.getUser();
        if(user.getRole()!=UserRole.USER&&user.getRole()!=UserRole.PATIENT)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Patient introuvable.");
        user.setStatus(request.status());return AdminPatientDto.from(patient);
    }

    @Transactional
    public void deleteDoctor(UUID id){
        Doctor doctor=doctors.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Médecin introuvable."));
        UUID doctorId=doctor.getId();DoctorApplication application=doctor.getApplication();UserAccount user=doctor.getUser();String storageKey=application.getDocumentStorageKey();
        deletion.deleteAbsences(doctorId);
        deletion.deleteAvailabilities(doctorId);
        deletion.deleteDoctorSpecialties(doctorId);
        deletion.deleteDoctor(doctorId);
        deletion.deleteDoctorApplication(application.getId());
        deletion.deleteUser(user.getId());
        if(storageKey!=null&&TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){documents.delete(storageKey);}});
    }

    @Transactional
    public void deletePatient(UUID id){
        Patient patient=patients.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Patient introuvable."));
        UserAccount user=patient.getUser();if(user.getRole()!=UserRole.USER&&user.getRole()!=UserRole.PATIENT)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Patient introuvable.");
        deletion.deletePatient(patient.getId());deletion.deleteUser(user.getId());
    }

    private Pageable pageRequest(int page,int size,String sort){int safePage=Math.max(0,page);int safeSize=Math.min(100,Math.max(1,size));return PageRequest.of(safePage,safeSize,Sort.by(Sort.Direction.ASC,sort));}
}
