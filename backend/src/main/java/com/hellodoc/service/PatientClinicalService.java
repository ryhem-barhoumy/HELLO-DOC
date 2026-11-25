package com.hellodoc.service;

import com.hellodoc.dto.PatientConsultationDto;
import com.hellodoc.dto.PrescriptionDto;
import com.hellodoc.entity.*;
import com.hellodoc.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PatientClinicalService {
    private final UserRepository users; private final AppointmentRepository appointments;
    private final ConsultationNoteRepository notes; private final PrescriptionRepository prescriptions;
    public PatientClinicalService(UserRepository users,AppointmentRepository appointments,ConsultationNoteRepository notes,PrescriptionRepository prescriptions){this.users=users;this.appointments=appointments;this.notes=notes;this.prescriptions=prescriptions;}

    @Transactional(readOnly=true)
    public List<PatientConsultationDto> myConsultations(String email){
        UserAccount user=users.findByEmailIgnoreCase(email).filter(u->(u.getRole()==UserRole.USER||u.getRole()==UserRole.PATIENT)&&"ACTIVE".equals(u.getStatus())).orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"Un compte patient actif est requis."));
        List<Appointment> visits=appointments.findPatientCompletedByUserId(user.getId()); if(visits.isEmpty()) return List.of();
        List<UUID> ids=visits.stream().map(Appointment::getId).toList();
        Map<UUID,ConsultationNote> noteMap=notes.findByAppointmentIdIn(ids).stream().filter(ConsultationNote::isFinalized).collect(Collectors.toMap(n->n.getAppointment().getId(),n->n));
        Map<UUID,List<PrescriptionDto>> rxMap=prescriptions.findByAppointmentIdInOrderByPrescribedAtDesc(ids).stream().filter(Prescription::isFinalized).map(PrescriptionDto::from).collect(Collectors.groupingBy(PrescriptionDto::appointmentId));
        return visits.stream().map(a->{
            ConsultationNote n=noteMap.get(a.getId());
            PatientConsultationDto.SharedNote shared=n==null?null:new PatientConsultationDto.SharedNote(n.getResume(),n.getObservation(),n.getTreatment(),n.getNextCheck(),n.getUpdatedAt(),n.getRecommendedDelay(),n.getFollowUpInstructions());
            UserAccount doctor=a.getDoctor().getUser();
            return new PatientConsultationDto(a.getId(),a.getDoctor().getId(),a.getStartAt(),a.getStatus().name(),a.getServiceName(),doctor.getFirstName()+" "+doctor.getLastName(),shared,rxMap.getOrDefault(a.getId(),List.of()));
        }).toList();
    }
}
