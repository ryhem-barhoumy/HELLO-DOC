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
    private final PrescriptionRepository prescriptions;
    private final DoctorProfileService profiles;
    private final PatientRepository patients;
    private final NotificationService notifications;
    public ConsultationService(AppointmentRepository appointments,ConsultationNoteRepository notes,ConsultationRevisionRepository revisions,PrescriptionRepository prescriptions,DoctorProfileService profiles,PatientRepository patients,NotificationService notifications){this.appointments=appointments;this.notes=notes;this.revisions=revisions;this.prescriptions=prescriptions;this.profiles=profiles;this.patients=patients;this.notifications=notifications;}
    @Transactional(readOnly=true)
    public ConsultationNoteDto note(String email,UUID id){ownedEditable(email,id,false);return notes.findByAppointmentId(id).map(ConsultationNoteDto::from).orElse(null);}
    @Transactional
    public ConsultationNoteDto save(String email,UUID id,ConsultationNoteRequest request){
        Appointment a=ownedEditable(email,id,true);
        ConsultationNote note=notes.findByAppointmentId(id).orElse(null);boolean created=note==null;
        if(created&&request.version()!=null)throw new ResponseStatusException(HttpStatus.CONFLICT,"La fiche a changé. Rechargez-la avant de réessayer.");
        if(!created&&(request.version()==null||request.version()!=note.getVersion()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Une autre modification a été enregistrée. Rechargez la fiche avant de réessayer ; vos saisies restent affichées.");
        boolean correcting=note!=null&&note.isFinalized();
        if(correcting&&(request.correctionReason()==null||request.correctionReason().isBlank()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Indiquez le motif de correction du compte rendu finalisé.");
        long previousVersion=note==null?0:note.getVersion();String previousResume=note==null?null:note.getResume(),previousObservation=note==null?null:note.getObservation(),previousTreatment=note==null?null:note.getTreatment(),previousPrivate=note==null?null:note.getPrivateNote();LocalDate previousCheck=note==null?null:note.getNextCheck();String previousDelay=note==null?null:note.getRecommendedDelay(),previousInstructions=note==null?null:note.getFollowUpInstructions();
        if(created)note=new ConsultationNote(a);
        if(request.nextCheck()!=null&&!request.nextCheck().equals(previousCheck)&&request.nextCheck().isBefore(LocalDate.now(ZoneId.of("Africa/Tunis")).plusDays(1)))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Le prochain contrôle doit être prévu à partir de demain.");
        note.update(request.resume(),request.observation(),request.treatment(),request.nextCheck(),request.recommendedDelay(),request.followUpInstructions(),request.privateNote());
        notes.saveAndFlush(note);var doctor=a.getDoctor().getUser();
        if(correcting)revisions.save(new ConsultationRevision(note,doctor.getFirstName()+" "+doctor.getLastName(),"CORRECTED",request.correctionReason().trim(),previousVersion,previousResume,previousObservation,previousTreatment,previousCheck,previousDelay,previousInstructions,previousPrivate));
        else revisions.save(new ConsultationRevision(note,doctor.getFirstName()+" "+doctor.getLastName(),created?"CREATED":"UPDATED"));
        boolean followUpChanged=!Objects.equals(previousCheck,request.nextCheck())||!Objects.equals(normalize(previousDelay),normalize(request.recommendedDelay()));
        if(followUpChanged&&(request.nextCheck()!=null||(request.recommendedDelay()!=null&&!request.recommendedDelay().isBlank()))){String when=request.nextCheck()!=null?" à partir du "+request.nextCheck():" dans "+request.recommendedDelay().trim();notifications.create(a.getPatient().getUser(),a,"FOLLOW_UP_RECOMMENDED","Contrôle conseillé","Votre médecin recommande un contrôle"+when+". Cette recommandation ne réserve pas de rendez-vous ; choisissez un créneau depuis son profil.");}
        return ConsultationNoteDto.from(note);
    }
    @Transactional
    public ConsultationNoteDto finalizeReport(String email,UUID id){
        Appointment a=ownedEditable(email,id,true);
        if(a.getStatus()!=AppointmentStatus.COMPLETED)throw new ResponseStatusException(HttpStatus.CONFLICT,"Le compte rendu ne peut être finalisé qu’après la fin du rendez-vous.");
        ConsultationNote note=notes.findByAppointmentId(id).orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Enregistrez d’abord un brouillon de compte rendu."));
        if(note.getResume().isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Un résumé clinique est requis pour finaliser le compte rendu.");
        if(note.isFinalized())return ConsultationNoteDto.from(note);
        note.finalizeReport();notes.saveAndFlush(note);var doctor=a.getDoctor().getUser();
        revisions.save(new ConsultationRevision(note,doctor.getFirstName()+" "+doctor.getLastName(),"FINALIZED"));
        return ConsultationNoteDto.from(note);
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> history(String email,UUID id){ownedEditable(email,id,false);return notes.findByAppointmentId(id).map(n->revisions.findTop50ByNoteIdOrderByChangedAtDesc(n.getId()).stream().map(r->{Map<String,Object> entry=new LinkedHashMap<>();entry.put("action",r.getAction());entry.put("version",r.getNoteVersion());entry.put("changedAt",r.getChangedAt());entry.put("editorName",r.getEditorName());entry.put("correctionReason",r.getCorrectionReason());entry.put("previousResume",r.getPreviousResume());entry.put("previousObservation",r.getPreviousObservation());entry.put("previousTreatment",r.getPreviousTreatment());entry.put("previousNextCheck",r.getPreviousNextCheck());entry.put("previousRecommendedDelay",r.getPreviousRecommendedDelay());entry.put("previousFollowUpInstructions",r.getPreviousFollowUpInstructions());entry.put("previousPrivateNote",r.getPreviousPrivateNote());return entry;}).toList()).orElse(List.of());}
    @Transactional(readOnly=true)
    public PagedResponse<DoctorPatientSummary> patients(String email,String q,int page,int size){Doctor d=profiles.own(email);String query=q==null||q.isBlank()?null:"%"+q.trim().toLowerCase(Locale.ROOT)+"%";return PagedResponse.from(appointments.findCompletedPatients(d.getId(),query,page(page,size)));}
    @Transactional(readOnly=true)
    public DoctorPatientDossier dossier(String email,UUID patientId,int page,int size){
        Doctor doctor=profiles.own(email);long count=appointments.countByDoctorIdAndPatientIdAndStatus(doctor.getId(),patientId,AppointmentStatus.COMPLETED);
        if(count==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Aucun suivi patient accessible pour ce compte.");
        Patient patient=patients.findById(patientId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Patient introuvable."));
        return dossierData(doctor,patient,page,size);
    }
    @Transactional(readOnly=true)
    public DoctorPatientDossier clinicalContext(String email,UUID appointmentId,int page,int size){
        Doctor doctor=profiles.own(email);Appointment appointment=appointments.findDoctorOwned(appointmentId,email).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Rendez-vous introuvable."));
        if(appointment.getStatus()!=AppointmentStatus.CONFIRMED&&appointment.getStatus()!=AppointmentStatus.COMPLETED)throw new ResponseStatusException(HttpStatus.CONFLICT,"Le contexte clinique est disponible pour un rendez-vous confirmé ou terminé.");
        return dossierData(doctor,appointment.getPatient(),page,size);
    }
    private DoctorPatientDossier dossierData(Doctor doctor,Patient patient,int page,int size){
        UUID patientId=patient.getId();long count=appointments.countByDoctorIdAndPatientIdAndStatus(doctor.getId(),patientId,AppointmentStatus.COMPLETED);
        var visits=appointments.findCompletedVisits(doctor.getId(),patientId,page(page,size));
        var ids=visits.getContent().stream().map(Appointment::getId).toList();
        var byVisit=ids.isEmpty()?Map.<UUID,ConsultationNote>of():notes.findByAppointmentIdIn(ids).stream().collect(Collectors.toMap(n->n.getAppointment().getId(),n->n));
        var rxByVisit=ids.isEmpty()?Map.<UUID,List<PrescriptionDto>>of():prescriptions.findByAppointmentIdInOrderByPrescribedAtDesc(ids).stream().map(PrescriptionDto::from).collect(Collectors.groupingBy(PrescriptionDto::appointmentId));
        var rows=visits.getContent().stream().map(a->new DoctorPatientDossier.Visit(a.getId(),a.getStartAt(),a.getEndAt(),a.getServiceName(),a.getStatus().name(),byVisit.containsKey(a.getId())?ConsultationNoteDto.from(byVisit.get(a.getId())):null,rxByVisit.getOrDefault(a.getId(),List.of()))).toList();
        var u=patient.getUser();return new DoctorPatientDossier(patientId,u.getFirstName()+" "+u.getLastName(),u.getEmail(),u.getPhone(),count,rows,visits.getNumber(),visits.getTotalPages());
    }
    private Appointment ownedEditable(String email,UUID id,boolean lock){profiles.own(email);Appointment a=(lock?appointments.lockDoctorOwned(id,email):appointments.findDoctorOwned(id,email)).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Consultation introuvable."));if(a.getStatus()!=AppointmentStatus.CONFIRMED&&a.getStatus()!=AppointmentStatus.COMPLETED)throw new ResponseStatusException(HttpStatus.CONFLICT,"La fiche est disponible pour un rendez-vous confirmé ou terminé.");return a;}
    private PageRequest page(int page,int size){return PageRequest.of(Math.max(0,page),Math.max(1,Math.min(50,size)));}
    private String normalize(String value){return value==null||value.isBlank()?null:value.trim();}
}
