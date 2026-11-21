package com.hellodoc.service;

import com.hellodoc.dto.*;
import com.hellodoc.entity.*;
import com.hellodoc.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConsultationService {
    private final AppointmentRepository appointments;
    private final ConsultationNoteRepository notes;
    private final ConsultationRevisionRepository revisions;
    private final DoctorProfileService profiles;
    private final PatientRepository patients;
    private final NotificationService notifications;
    public ConsultationService(AppointmentRepository appointments,ConsultationNoteRepository notes,ConsultationRevisionRepository revisions,DoctorProfileService profiles,PatientRepository patients,NotificationService notifications){this.appointments=appointments;this.notes=notes;this.revisions=revisions;this.profiles=profiles;this.patients=patients;this.notifications=notifications;}
    @Transactional(readOnly=true)
    public ConsultationNoteDto note(String email,UUID id){ownedCompleted(email,id,false);return notes.findByAppointmentId(id).map(ConsultationNoteDto::from).orElse(null);}
    @Transactional
    public ConsultationNoteDto save(String email,UUID id,ConsultationNoteRequest request){
        Appointment a=ownedCompleted(email,id,true);
        ConsultationNote note=notes.findByAppointmentId(id).orElse(null);boolean created=note==null;
        if(created&&request.version()!=null)throw new ResponseStatusException(HttpStatus.CONFLICT,"La fiche a changé. Rechargez-la avant de réessayer.");
        if(!created&&(request.version()==null||request.version()!=note.getVersion()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Une autre modification a été enregistrée. Rechargez la fiche avant de réessayer ; vos saisies restent affichées.");
        if(created)note=new ConsultationNote(a);
        LocalDate previousCheck=note.getNextCheck();
        if(request.nextCheck()!=null&&!request.nextCheck().equals(previousCheck)&&request.nextCheck().isBefore(LocalDate.now(ZoneId.of("Africa/Tunis")).plusDays(1)))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Le prochain contrôle doit être prévu à partir de demain.");
        note.update(request.resume(),request.observation(),request.treatment(),request.nextCheck(),request.privateNote());
        notes.saveAndFlush(note);var doctor=a.getDoctor().getUser();
        revisions.save(new ConsultationRevision(note,doctor.getFirstName()+" "+doctor.getLastName(),created?"CREATED":"UPDATED"));
        if(request.nextCheck()!=null&&!request.nextCheck().equals(previousCheck))notifications.create(a.getPatient().getUser(),a,"FOLLOW_UP_RECOMMENDED","Contrôle conseillé","Votre médecin vous invite à prendre un rendez-vous de contrôle à partir du "+request.nextCheck()+". Choisissez une disponibilité depuis son profil.");
        return ConsultationNoteDto.from(note);
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> history(String email,UUID id){ownedCompleted(email,id,false);return notes.findByAppointmentId(id).map(n->revisions.findTop50ByNoteIdOrderByChangedAtDesc(n.getId()).stream().map(r->Map.<String,Object>of("action",r.getAction(),"version",r.getNoteVersion(),"changedAt",r.getChangedAt(),"editorName",r.getEditorName())).toList()).orElse(List.of());}
    @Transactional(readOnly=true)
    public PagedResponse<DoctorPatientSummary> patients(String email,String q,int page,int size){Doctor d=profiles.own(email);String query=q==null||q.isBlank()?null:"%"+q.trim().toLowerCase(Locale.ROOT)+"%";return PagedResponse.from(appointments.findCompletedPatients(d.getId(),query,page(page,size)));}
    @Transactional(readOnly=true)
    public DoctorPatientDossier dossier(String email,UUID patientId,int page,int size){
        Doctor doctor=profiles.own(email);long count=appointments.countByDoctorIdAndPatientIdAndStatus(doctor.getId(),patientId,AppointmentStatus.COMPLETED);
        if(count==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Aucun suivi patient accessible pour ce compte.");
        Patient patient=patients.findById(patientId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Patient introuvable."));
        var visits=appointments.findCompletedVisits(doctor.getId(),patientId,page(page,size));
        var ids=visits.getContent().stream().map(Appointment::getId).toList();
        var byVisit=ids.isEmpty()?Map.<UUID,ConsultationNote>of():notes.findByAppointmentIdIn(ids).stream().collect(Collectors.toMap(n->n.getAppointment().getId(),n->n));
        var rows=visits.getContent().stream().map(a->new DoctorPatientDossier.Visit(a.getId(),a.getStartAt(),a.getEndAt(),a.getServiceName(),a.getStatus().name(),byVisit.containsKey(a.getId())?ConsultationNoteDto.from(byVisit.get(a.getId())):null)).toList();
        var u=patient.getUser();return new DoctorPatientDossier(patientId,u.getFirstName()+" "+u.getLastName(),u.getEmail(),u.getPhone(),count,rows,visits.getNumber(),visits.getTotalPages());
    }
    private Appointment ownedCompleted(String email,UUID id,boolean lock){profiles.own(email);Appointment a=(lock?appointments.lockDoctorOwned(id,email):appointments.findDoctorOwned(id,email)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Consultation introuvable."));if(a.getStatus()!=AppointmentStatus.COMPLETED)throw new ResponseStatusException(HttpStatus.CONFLICT,"La fiche est disponible uniquement après une consultation terminée.");return a;}
    private PageRequest page(int page,int size){return PageRequest.of(Math.max(0,page),Math.max(1,Math.min(50,size)));}
}
