package com.hellodoc.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellodoc.dto.*;
import com.hellodoc.entity.*;
import com.hellodoc.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@Service
public class AppointmentService {
    private static final ZoneId BUSINESS_ZONE=ZoneId.of("Africa/Tunis");
    private static final List<AppointmentStatus> ACTIVE=List.of(AppointmentStatus.PENDING,AppointmentStatus.CONFIRMED);
    private static final List<AppointmentStatus> FINISHED=List.of(AppointmentStatus.COMPLETED,AppointmentStatus.REJECTED,AppointmentStatus.CANCELLED,AppointmentStatus.NO_SHOW);
    private final AppointmentRepository appointments; private final AppointmentStatusHistoryRepository history; private final ReviewRepository reviews; private final DoctorRepository doctors; private final PatientRepository patients; private final UserRepository users; private final DoctorProfileService doctorProfiles; private final NotificationService notifications; private final ObjectMapper json;
    public AppointmentService(AppointmentRepository appointments,AppointmentStatusHistoryRepository history,ReviewRepository reviews,DoctorRepository doctors,PatientRepository patients,UserRepository users,DoctorProfileService doctorProfiles,NotificationService notifications,ObjectMapper json){this.appointments=appointments;this.history=history;this.reviews=reviews;this.doctors=doctors;this.patients=patients;this.users=users;this.doctorProfiles=doctorProfiles;this.notifications=notifications;this.json=json;}

    @Transactional
    public AppointmentDto book(String email,AppointmentCreateRequest request){
        UserAccount actor=patientUser(email);
        Doctor doctor=doctors.lockActiveById(request.doctorId()).filter(d->d.getUser().getRole()==UserRole.DOCTOR&&"ACTIVE".equals(d.getUser().getStatus())).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Profil médecin actif introuvable."));
        Patient patient=patients.findByUserId(actor.getId()).orElseGet(()->patients.save(new Patient(actor)));
        String serviceName=request.serviceName().trim();MedicalServiceDto service=findService(doctor,serviceName);
        LocalDate date=request.startAt().atZone(BUSINESS_ZONE).toLocalDate();TimeSlotDto slot=availableSlots(doctor.getId(),date,null).stream().filter(s->s.startsAt().equals(request.startAt())).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Ce créneau n’est plus disponible. Choisissez une autre heure."));
        Appointment appointment=new Appointment(doctor,patient,service.name(),service.price(),slot.startsAt(),slot.endsAt(),clean(request.reason()));
        try{appointment=appointments.saveAndFlush(appointment);}catch(DataIntegrityViolationException e){throw new ResponseStatusException(HttpStatus.CONFLICT,"Ce créneau vient d’être réservé. Choisissez-en un autre.");}
        record(appointment,actor,null,AppointmentStatus.PENDING,"Demande de rendez-vous créée.");
        notifications.create(doctor.getUser(),appointment,"APPOINTMENT_REQUEST","Nouvelle demande de rendez-vous",actor.getFirstName()+" "+actor.getLastName()+" demande "+service.name()+" le "+slot.startsAt().atZone(BUSINESS_ZONE).toLocalDateTime()+".");
        return dto(appointment);
    }

    @Transactional(readOnly=true)
    public PagedResponse<AppointmentDto> patientAppointments(String email,boolean historyView,int page,int size){patientUser(email);Page<Appointment> found=appointments.findPatientAppointments(email,historyView,Instant.now(),ACTIVE,FINISHED,PageRequest.of(safePage(page),safeSize(size)));return PagedResponse.from(found.map(this::dto));}

    @Transactional(readOnly=true)
    public PagedResponse<AppointmentDto> doctorAppointments(String email,Instant from,Instant to,int page,int size){Doctor doctor=doctorProfiles.own(email);if(from!=null&&to!=null&&!to.isAfter(from))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La fin de période doit suivre son début.");Page<Appointment> found=appointments.findDoctorAppointments(doctor.getId(),from,to,PageRequest.of(safePage(page),safeSize(size),Sort.by("startAt").ascending()));return PagedResponse.from(found.map(this::dto));}

    @Transactional(readOnly=true)
    public AppointmentDto get(String email,UUID id){UserAccount actor=activeUser(email);validateRoleStatus(actor);Appointment appointment=owned(email,id);return dto(appointment);}

    @Transactional
    public AppointmentDto changeStatus(String email,UUID id,AppointmentStatusRequest request){
        UserAccount actor=activeUser(email);if(actor.getRole()!=UserRole.DOCTOR)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Seul le médecin concerné peut modifier le statut.");doctorProfiles.own(email);
        Appointment a=owned(email,id);AppointmentStatus old=a.getStatus(),next=request.status();
        if(next==AppointmentStatus.CANCELLED)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Utilisez l’action d’annulation pour préciser le motif.");
        boolean allowed=(old==AppointmentStatus.PENDING&&(next==AppointmentStatus.CONFIRMED||next==AppointmentStatus.REJECTED))||(old==AppointmentStatus.CONFIRMED&&(next==AppointmentStatus.COMPLETED||next==AppointmentStatus.NO_SHOW));
        if(!allowed)throw new ResponseStatusException(HttpStatus.CONFLICT,"Ce changement de statut n’est pas autorisé.");
        Instant now=Instant.now();if(next==AppointmentStatus.CONFIRMED&&!a.getStartAt().isAfter(now))throw new ResponseStatusException(HttpStatus.CONFLICT,"Ce rendez-vous est déjà passé et ne peut plus être confirmé.");if(next==AppointmentStatus.COMPLETED&&a.getEndAt().isAfter(now))throw new ResponseStatusException(HttpStatus.CONFLICT,"La consultation ne peut être terminée qu’après son heure de fin.");if(next==AppointmentStatus.NO_SHOW&&a.getStartAt().isAfter(now))throw new ResponseStatusException(HttpStatus.CONFLICT,"L’absence ne peut être enregistrée qu’après l’heure prévue du rendez-vous.");
        a.changeStatus(next,clean(request.reason()));record(a,actor,old,next,clean(request.reason()));notifications.create(a.getPatient().getUser(),a,"APPOINTMENT_"+next.name(),statusTitle(next),statusMessage(next,a));return dto(a);
    }

    @Transactional
    public AppointmentDto cancel(String email,UUID id,String reason){UserAccount actor=activeUser(email);if(actor.getRole()==UserRole.DOCTOR)doctorProfiles.own(email);else if(actor.getRole()!=UserRole.USER&&actor.getRole()!=UserRole.PATIENT)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Seul le patient ou le médecin du rendez-vous peut l’annuler.");Appointment a=owned(email,id);if(!ACTIVE.contains(a.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Ce rendez-vous ne peut plus être annulé.");if(!a.getStartAt().isAfter(Instant.now()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Un rendez-vous déjà commencé ne peut plus être annulé.");AppointmentStatus old=a.getStatus();a.changeStatus(AppointmentStatus.CANCELLED,clean(reason));record(a,actor,old,AppointmentStatus.CANCELLED,clean(reason));UserAccount recipient=actor.getRole()==UserRole.DOCTOR?a.getPatient().getUser():a.getDoctor().getUser();notifications.create(recipient,a,"APPOINTMENT_CANCELLED","Rendez-vous annulé",actor.getFirstName()+" "+actor.getLastName()+" a annulé le rendez-vous du "+a.getStartAt().atZone(BUSINESS_ZONE).toLocalDateTime()+".");return dto(a);}

    @Transactional(readOnly=true)
    public List<TimeSlotDto> rescheduleOptions(String email,UUID id,LocalDate date){UserAccount actor=activeUser(email);validateRoleStatus(actor);Appointment a=owned(email,id);assertReschedulable(a);if(a.getDoctor().getUser().getRole()!=UserRole.DOCTOR||!"ACTIVE".equals(a.getDoctor().getUser().getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Le profil médecin n’est actuellement pas disponible.");return availableSlots(a.getDoctor().getId(),date,a.getId()).stream().filter(s->!s.startsAt().equals(a.getStartAt())).toList();}

    @Transactional
    public AppointmentDto reschedule(String email,UUID id,AppointmentRescheduleRequest request){
        UserAccount actor=activeUser(email);Appointment a=owned(email,id);if(actor.getRole()==UserRole.DOCTOR)doctorProfiles.own(email);else if(actor.getRole()!=UserRole.USER&&actor.getRole()!=UserRole.PATIENT)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Action réservée au patient ou au médecin concerné.");assertReschedulable(a);
        Doctor doctor=doctors.lockActiveById(a.getDoctor().getId()).filter(d->d.getUser().getRole()==UserRole.DOCTOR&&"ACTIVE".equals(d.getUser().getStatus())).orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Le profil médecin n’est actuellement pas disponible."));
        LocalDate date=request.startAt().atZone(BUSINESS_ZONE).toLocalDate();TimeSlotDto slot=availableSlots(doctor.getId(),date,a.getId()).stream().filter(s->s.startsAt().equals(request.startAt())).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Le nouveau créneau n’est plus disponible."));
        a.reschedule(slot.startsAt(),slot.endsAt(),clean(request.reason()));UserAccount recipient=actor.getRole()==UserRole.DOCTOR?a.getPatient().getUser():a.getDoctor().getUser();notifications.create(recipient,a,"APPOINTMENT_RESCHEDULED","Rendez-vous reprogrammé","Le rendez-vous a été déplacé au "+slot.startsAt().atZone(BUSINESS_ZONE).toLocalDateTime()+".");return dto(a);
    }

    @Transactional(readOnly=true)
    public PatientDashboardDto patientDashboard(String email){UserAccount user=patientUser(email);Instant now=Instant.now();Appointment next=appointments.findNextPatientAppointments(email,now,ACTIVE,PageRequest.of(0,1)).stream().findFirst().orElse(null);long upcoming=appointments.countPatientUpcoming(user.getId(),now,ACTIVE);long past=appointments.countPatientPast(user.getId(),now,FINISHED);return new PatientDashboardDto(upcoming,past,notifications.unreadCount(email),next==null?null:dto(next));}

    @Transactional(readOnly=true)
    public DoctorDashboardDto doctorDashboard(String email){Doctor doctor=doctorProfiles.own(email);Instant now=Instant.now();LocalDate today=LocalDate.now(BUSINESS_ZONE);List<AppointmentDto> next=appointments.findUpcomingDoctorAppointments(doctor.getId(),now,ACTIVE,PageRequest.of(0,5)).stream().map(this::dto).toList();return new DoctorDashboardDto(appointments.countDoctorUpcomingStatus(doctor.getId(),AppointmentStatus.PENDING,now),appointments.countDoctorUpcomingStatus(doctor.getId(),AppointmentStatus.CONFIRMED,now),appointments.countByDoctorIdAndStatus(doctor.getId(),AppointmentStatus.COMPLETED),appointments.countDoctorToday(doctor.getId(),today.atStartOfDay(BUSINESS_ZONE).toInstant(),today.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant(),ACTIVE),notifications.unreadCount(email),next);}

    public List<TimeSlotDto> freeSlots(UUID doctorId,LocalDate date){return availableSlots(doctorId,date,null);}
    private List<TimeSlotDto> availableSlots(UUID doctorId,LocalDate date,UUID ignoredAppointment){List<TimeSlotDto> slots=doctorProfiles.slots(doctorId,date);if(slots.isEmpty())return slots;Instant from=date.atStartOfDay(BUSINESS_ZONE).toInstant(),to=date.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant();List<Appointment> occupied=appointments.findOverlapping(doctorId,ACTIVE,from,to);return slots.stream().filter(slot->occupied.stream().filter(a->!a.getId().equals(ignoredAppointment)).noneMatch(a->a.getStartAt().isBefore(slot.endsAt())&&a.getEndAt().isAfter(slot.startsAt()))).toList();}
    private MedicalServiceDto findService(Doctor doctor,String name){try{List<MedicalServiceDto> services=json.readValue(doctor.getServicesJson(),new TypeReference<>(){});return services.stream().filter(s->s.name()!=null&&s.name().trim().equalsIgnoreCase(name)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Le service choisi n’est pas proposé par ce médecin."));}catch(ResponseStatusException e){throw e;}catch(Exception e){throw new ResponseStatusException(HttpStatus.CONFLICT,"Les services du médecin ne sont pas disponibles actuellement.");}}
    private Appointment owned(String email,UUID id){return appointments.findOwned(id,email).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Rendez-vous introuvable."));}
    private UserAccount patientUser(String email){UserAccount user=activeUser(email);if(user.getRole()!=UserRole.USER&&user.getRole()!=UserRole.PATIENT)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Un compte patient actif est requis.");return user;}
    private void validateRoleStatus(UserAccount user){if(user.getRole()==UserRole.DOCTOR)doctorProfiles.own(user.getEmail());else if(user.getRole()!=UserRole.USER&&user.getRole()!=UserRole.PATIENT)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Action réservée au patient ou au médecin concerné.");}
    private UserAccount activeUser(String email){UserAccount user=users.findByEmailIgnoreCase(email).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentification requise."));if(!"ACTIVE".equals(user.getStatus()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Un compte actif est requis.");return user;}
    private void assertReschedulable(Appointment a){if(!ACTIVE.contains(a.getStatus()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Seuls les rendez-vous en attente ou confirmés peuvent être reprogrammés.");}
    private void record(Appointment a,UserAccount actor,AppointmentStatus old,AppointmentStatus next,String reason){history.save(new AppointmentStatusHistory(a,actor,old,next,reason));}
    private AppointmentDto dto(Appointment a){List<AppointmentStatusHistoryDto> changes=history.findByAppointmentIdOrderByChangedAtAsc(a.getId()).stream().map(h->new AppointmentStatusHistoryDto(h.getOldStatus(),h.getNewStatus(),h.getChangedAt(),h.getChangedBy().getFirstName()+" "+h.getChangedBy().getLastName(),h.getReason())).toList();ReviewDto review=reviews.findByAppointmentId(a.getId()).map(ReviewDto::from).orElse(null);return AppointmentDto.from(a,changes,review);}
    private String statusTitle(AppointmentStatus status){return switch(status){case CONFIRMED->"Rendez-vous confirmé";case REJECTED->"Demande de rendez-vous refusée";case COMPLETED->"Consultation terminée";case NO_SHOW->"Absence au rendez-vous";default->"Mise à jour du rendez-vous";};}
    private String statusMessage(AppointmentStatus status,Appointment a){return switch(status){case CONFIRMED->"Votre rendez-vous du "+a.getStartAt().atZone(BUSINESS_ZONE).toLocalDateTime()+" est confirmé.";case REJECTED->"Le médecin ne peut pas accepter le rendez-vous du "+a.getStartAt().atZone(BUSINESS_ZONE).toLocalDateTime()+".";case COMPLETED->"La consultation du "+a.getStartAt().atZone(BUSINESS_ZONE).toLocalDateTime()+" est terminée.";case NO_SHOW->"Le médecin a indiqué votre absence au rendez-vous du "+a.getStartAt().atZone(BUSINESS_ZONE).toLocalDateTime()+".";default->"Le statut de votre rendez-vous a été mis à jour.";};}
    private int safePage(int page){return Math.max(0,page);}private int safeSize(int size){return Math.max(1,Math.min(size,100));}private String clean(String value){return value==null||value.isBlank()?null:value.trim();}
}
