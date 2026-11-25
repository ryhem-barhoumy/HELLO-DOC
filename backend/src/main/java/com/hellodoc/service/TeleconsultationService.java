package com.hellodoc.service;

import com.hellodoc.dto.TeleconsultationDto;
import com.hellodoc.entity.*;
import com.hellodoc.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class TeleconsultationService {
    private static final java.time.Duration OPEN_EARLY=java.time.Duration.ofMinutes(15);
    private static final java.time.Duration LATE_GRACE=java.time.Duration.ofMinutes(30);
    private final TeleconsultationSessionRepository sessions; private final AppointmentRepository appointments;
    private final UserRepository users; private final DoctorProfileService doctors;
    public TeleconsultationService(TeleconsultationSessionRepository sessions,AppointmentRepository appointments,UserRepository users,DoctorProfileService doctors){this.sessions=sessions;this.appointments=appointments;this.users=users;this.doctors=doctors;}

    @Transactional
    public TeleconsultationDto create(String email,UUID appointmentId){
        doctors.own(email);
        Appointment appointment=sessions.lockDoctorAppointment(appointmentId,email).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Rendez-vous introuvable."));
        if(appointment.getStatus()!=AppointmentStatus.CONFIRMED)throw new ResponseStatusException(HttpStatus.CONFLICT,"La téléconsultation nécessite un rendez-vous confirmé.");
        if(appointment.getConsultationMode()!=ConsultationMode.VIDEO)throw new ResponseStatusException(HttpStatus.CONFLICT,"Une séance vidéo exige un rendez-vous prévu en mode téléconsultation.");
        TeleconsultationSession session=sessions.findByAppointmentId(appointmentId).orElse(null);
        if(session!=null&&session.getState()==TeleconsultationSession.State.ACTIVE)return TeleconsultationDto.from(session);
        ensureWindow(appointment.getStartAt());
        if(session==null)session=sessions.save(new TeleconsultationSession(appointment));
        if(session.getState()==TeleconsultationSession.State.COMPLETED)throw new ResponseStatusException(HttpStatus.CONFLICT,"Cette séance vidéo est terminée.");
        return TeleconsultationDto.from(session);
    }
    @Transactional(readOnly=true)
    public TeleconsultationDto get(String email,UUID id){return TeleconsultationDto.from(authorized(email,id));}
    @Transactional(readOnly=true)
    public TeleconsultationDto getForAppointment(String email,UUID appointmentId){
        UserAccount actor=users.findByEmailIgnoreCase(email).filter(u->"ACTIVE".equals(u.getStatus())).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentification requise."));
        Appointment a=appointments.findOwned(appointmentId,email).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Rendez-vous introuvable."));
        if(actor.getRole()==UserRole.DOCTOR)doctors.own(email);
        if(a.getConsultationMode()!=ConsultationMode.VIDEO)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Ce rendez-vous est prévu au cabinet.");
        TeleconsultationSession session=sessions.findByAppointmentId(a.getId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Aucune séance n’a été créée pour ce rendez-vous."));ensureSessionAccessible(session);return TeleconsultationDto.from(session);
    }
    @Transactional
    public TeleconsultationDto start(String email,UUID id){
        TeleconsultationSession session=authorized(email,id);if(session.getAppointment().getDoctor().getUser().getEmail().equalsIgnoreCase(email)){
            if(session.getAppointment().getStatus()!=AppointmentStatus.CONFIRMED)throw new ResponseStatusException(HttpStatus.CONFLICT,"Le rendez-vous n’est plus confirmé.");
            ensureWindow(session.getAppointment().getStartAt());
            if(session.getState()==TeleconsultationSession.State.COMPLETED)throw new ResponseStatusException(HttpStatus.CONFLICT,"La séance est terminée.");
            session.start();return TeleconsultationDto.from(session);
        }throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Seul le médecin du rendez-vous peut démarrer la séance.");
    }
    @Transactional
    public TeleconsultationDto complete(String email,UUID id){
        TeleconsultationSession s=authorized(email,id);if(!s.getAppointment().getDoctor().getUser().getEmail().equalsIgnoreCase(email))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Seul le médecin du rendez-vous peut terminer la séance.");
        if(s.getState()==TeleconsultationSession.State.WAITING)throw new ResponseStatusException(HttpStatus.CONFLICT,"La séance doit être démarrée avant sa clôture.");
        s.complete();return TeleconsultationDto.from(s);
    }
    @Transactional(readOnly=true)
    public TeleconsultationSession authorized(String email,UUID id){
        UserAccount actor=users.findByEmailIgnoreCase(email).filter(u->"ACTIVE".equals(u.getStatus())).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentification requise."));
        TeleconsultationSession s=sessions.findParticipantContext(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Séance introuvable."));
        Appointment a=s.getAppointment();UUID actorId=actor.getId();UUID doctorId=a.getDoctor().getUser().getId();UUID patientId=a.getPatient().getUser().getId();
        if(a.getConsultationMode()!=ConsultationMode.VIDEO)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Aucune séance vidéo n’est associée à ce rendez-vous.");
        boolean doctor=actor.getRole()==UserRole.DOCTOR&&actorId!=null&&Objects.equals(doctorId,actorId);
        boolean patient=(actor.getRole()==UserRole.USER||actor.getRole()==UserRole.PATIENT)&&actorId!=null&&Objects.equals(patientId,actorId);
        if(!doctor&&!patient)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Séance introuvable.");
        if(a.getStatus()!=AppointmentStatus.CONFIRMED&&a.getStatus()!=AppointmentStatus.COMPLETED)throw new ResponseStatusException(HttpStatus.CONFLICT,"Le rendez-vous n’est plus éligible.");
        ensureSessionAccessible(s);
        return s;
    }
    private void ensureSessionAccessible(TeleconsultationSession session){if(session.getState()==TeleconsultationSession.State.WAITING)ensureWindow(session.getAppointment().getStartAt());}
    static boolean withinAccessWindow(Instant startsAt,Instant now){return !now.isBefore(startsAt.minus(OPEN_EARLY))&&!now.isAfter(startsAt.plus(LATE_GRACE));}
    private void ensureWindow(Instant startsAt){if(!withinAccessWindow(startsAt,Instant.now()))throw new ResponseStatusException(HttpStatus.CONFLICT,"La salle vidéo ouvre 15 minutes avant le rendez-vous et reste accessible jusqu’à 30 minutes après son début.");}
}
