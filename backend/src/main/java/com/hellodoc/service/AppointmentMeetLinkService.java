package com.hellodoc.service;

import com.hellodoc.dto.MeetLinkDto;
import com.hellodoc.dto.MeetLinkRequest;
import com.hellodoc.entity.Appointment;
import com.hellodoc.entity.AppointmentStatus;
import com.hellodoc.entity.ConsultationMode;
import com.hellodoc.repository.AppointmentRepository;
import com.hellodoc.repository.TeleconsultationSessionRepository;
import com.hellodoc.entity.TeleconsultationSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AppointmentMeetLinkService {
    private static final Pattern MEET_PATH=Pattern.compile("/(?:[a-z]{3}-[a-z]{4}-[a-z]{3}|lookup/[A-Za-z0-9_-]{8,})",Pattern.CASE_INSENSITIVE);
    private final AppointmentRepository appointments;
    private final DoctorProfileService doctors;
    private final TeleconsultationSessionRepository sessions;

    @org.springframework.beans.factory.annotation.Autowired
    public AppointmentMeetLinkService(AppointmentRepository appointments,DoctorProfileService doctors,TeleconsultationSessionRepository sessions){this.appointments=appointments;this.doctors=doctors;this.sessions=sessions;}
    public AppointmentMeetLinkService(AppointmentRepository appointments,DoctorProfileService doctors){this(appointments,doctors,null);}

    @Transactional(readOnly=true)
    public MeetLinkDto get(String email,UUID appointmentId){
        Appointment a=appointments.findOwned(appointmentId,email).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Rendez-vous introuvable."));
        requireVideoAppointment(a,false);
        if(a.getPatient().getUser().getEmail().equalsIgnoreCase(email)){
            Instant now=Instant.now();
            if(a.getStatus()!=AppointmentStatus.CONFIRMED||now.isBefore(a.getStartAt().minusSeconds(15*60L))||now.isAfter(a.getStartAt().plusSeconds(30*60L)))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Le lien de secours n’est pas disponible à cette heure.");
        }
        return dto(a);
    }

    @Transactional
    public MeetLinkDto save(String email,UUID appointmentId,MeetLinkRequest request){
        doctors.own(email);
        Appointment a=appointments.findDoctorOwned(appointmentId,email).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Rendez-vous introuvable."));
        requireVideoAppointment(a,true);
        String normalized=validate(request.url());
        a.setMeetLink(normalized);
        return dto(a);
    }

    @Transactional
    public void remove(String email,UUID appointmentId){
        doctors.own(email);
        Appointment a=appointments.findDoctorOwned(appointmentId,email).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Rendez-vous introuvable."));
        requireVideoAppointment(a,true);
        a.setMeetLink(null);
    }

    @Transactional
    public MeetLinkDto markExternalCallEnded(String email,UUID appointmentId){
        doctors.own(email);
        Appointment a=appointments.findDoctorOwned(appointmentId,email).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Rendez-vous introuvable."));
        requireVideoAppointment(a,true);
        if(a.getMeetLink()==null||a.getMeetLink().isBlank())throw new ResponseStatusException(HttpStatus.CONFLICT,"Ajoutez d’abord un lien de secours Google Meet.");
        if(Instant.now().isBefore(a.getStartAt().minusSeconds(15*60L)))throw new ResponseStatusException(HttpStatus.CONFLICT,"La confirmation de fin Meet est disponible à partir de l’ouverture de la salle, 15 minutes avant le rendez-vous.");
        if(sessions!=null){var session=sessions.findByAppointmentId(a.getId()).orElse(null);if(session!=null&&session.getState()==TeleconsultationSession.State.ACTIVE)throw new ResponseStatusException(HttpStatus.CONFLICT,"Terminez d’abord la séance WebRTC active.");}
        a.markMeetCallEnded(email);
        return dto(a);
    }

    private MeetLinkDto dto(Appointment a){return new MeetLinkDto(a.getId(),a.getMeetLink(),a.getMeetCallEndedAt(),a.getMeetCallEndedBy());}

    private void requireVideoAppointment(Appointment a,boolean confirmedOnly){
        if(a.getConsultationMode()!=ConsultationMode.VIDEO)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Ce rendez-vous n’est pas une consultation vidéo.");
        boolean eligible=confirmedOnly?a.getStatus()==AppointmentStatus.CONFIRMED:(a.getStatus()==AppointmentStatus.CONFIRMED||a.getStatus()==AppointmentStatus.COMPLETED);
        if(!eligible)throw new ResponseStatusException(HttpStatus.CONFLICT,"Le lien de secours est disponible pour un rendez-vous vidéo confirmé.");
    }

    private String validate(String raw){
        try{
            URI uri=URI.create(raw.trim());
            String path=uri.getPath()==null?"":uri.getPath();
            if(!"https".equalsIgnoreCase(uri.getScheme())||uri.getHost()==null||!"meet.google.com".equals(uri.getHost().toLowerCase(Locale.ROOT))||uri.getUserInfo()!=null||(uri.getPort()!=-1&&uri.getPort()!=443)||uri.getQuery()!=null||uri.getFragment()!=null||!MEET_PATH.matcher(path).matches())throw new IllegalArgumentException();
            return uri.toASCIIString();
        }catch(RuntimeException ex){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Utilisez un lien HTTPS Google Meet valide, sans paramètres ni fragment.");}
    }
}
