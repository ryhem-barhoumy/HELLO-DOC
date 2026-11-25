package com.hellodoc.service;

import com.hellodoc.dto.PrescriptionDto;
import com.hellodoc.dto.PrescriptionRequest;
import com.hellodoc.entity.*;
import com.hellodoc.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;

@Service
public class PrescriptionService {
    private final AppointmentRepository appointments; private final PrescriptionRepository prescriptions;
    private final DoctorProfileService doctors; private final UserRepository users;
    public PrescriptionService(AppointmentRepository appointments, PrescriptionRepository prescriptions, DoctorProfileService doctors, UserRepository users){this.appointments=appointments;this.prescriptions=prescriptions;this.doctors=doctors;this.users=users;}

    @Transactional
    public PrescriptionDto create(String email, UUID appointmentId, PrescriptionRequest request){
        doctors.own(email);
        Appointment appointment=appointments.findDoctorOwned(appointmentId,email).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Consultation introuvable."));
        if(appointment.getStatus()!=AppointmentStatus.CONFIRMED&&appointment.getStatus()!=AppointmentStatus.COMPLETED) throw new ResponseStatusException(HttpStatus.CONFLICT,"Une ordonnance peut être préparée pour un rendez-vous confirmé ou terminé.");
        var items=request.items().stream().map(i->new PrescriptionItem(i.medication(),i.instructions())).toList();
        return PrescriptionDto.from(prescriptions.save(new Prescription(appointment,request.recommendations(),items,null)));
    }

    @Transactional
    public PrescriptionDto amend(String email,UUID prescriptionId,PrescriptionRequest request){
        doctors.own(email);Prescription original=prescriptions.findById(prescriptionId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Ordonnance introuvable."));
        Appointment appointment=appointments.findDoctorOwned(original.getAppointment().getId(),email).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Ordonnance introuvable."));
        if(appointment.getStatus()!=AppointmentStatus.CONFIRMED&&appointment.getStatus()!=AppointmentStatus.COMPLETED)throw new ResponseStatusException(HttpStatus.CONFLICT,"Une ordonnance ne peut être corrigée que pour un rendez-vous confirmé ou terminé.");
        var items=request.items().stream().map(i->new PrescriptionItem(i.medication(),i.instructions())).toList();
        return PrescriptionDto.from(prescriptions.save(new Prescription(appointment,request.recommendations(),items,original)));
    }

    @Transactional
    public PrescriptionDto finalizePrescription(String email,UUID id){
        doctors.own(email);Prescription p=prescriptions.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Ordonnance introuvable."));
        Appointment a=appointments.findDoctorOwned(p.getAppointment().getId(),email).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Ordonnance introuvable."));
        if(a.getStatus()!=AppointmentStatus.CONFIRMED&&a.getStatus()!=AppointmentStatus.COMPLETED)throw new ResponseStatusException(HttpStatus.CONFLICT,"Le rendez-vous n’est plus éligible à cette ordonnance.");
        if(!p.isFinalized()){p.finalizePrescription();prescriptions.flush();}
        return PrescriptionDto.from(p);
    }

    @Transactional(readOnly=true)
    public List<PrescriptionDto> forAppointmentDoctor(String email, UUID id){
        doctors.own(email); Appointment a=appointments.findDoctorOwned(id,email).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Consultation introuvable."));
        return prescriptions.findByAppointmentIdInOrderByPrescribedAtDesc(List.of(a.getId())).stream().map(PrescriptionDto::from).toList();
    }

    @Transactional(readOnly=true)
    public List<PrescriptionDto> forPatient(String email){
        UserAccount user=users.findByEmailIgnoreCase(email).filter(u->(u.getRole()==UserRole.USER||u.getRole()==UserRole.PATIENT)&&"ACTIVE".equals(u.getStatus())).orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"Un compte patient actif est requis."));
        List<Appointment> visits=appointments.findPatientCompletedByUserId(user.getId());
        if(visits.isEmpty()) return List.of();
        return prescriptions.findByAppointmentIdInOrderByPrescribedAtDesc(visits.stream().map(Appointment::getId).toList()).stream().filter(Prescription::isFinalized).map(PrescriptionDto::from).toList();
    }
}
