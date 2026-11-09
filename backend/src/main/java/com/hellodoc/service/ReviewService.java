package com.hellodoc.service;

import com.hellodoc.dto.*;
import com.hellodoc.entity.*;
import com.hellodoc.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@Service
public class ReviewService {
 private final ReviewRepository reviews;private final AppointmentRepository appointments;private final PatientRepository patients;private final UserRepository users;private final DoctorProfileService doctorProfiles;private final NotificationService notifications;
 public ReviewService(ReviewRepository reviews,AppointmentRepository appointments,PatientRepository patients,UserRepository users,DoctorProfileService doctorProfiles,NotificationService notifications){this.reviews=reviews;this.appointments=appointments;this.patients=patients;this.users=users;this.doctorProfiles=doctorProfiles;this.notifications=notifications;}
 @Transactional public ReviewDto save(String email,UUID appointmentId,ReviewCreateRequest request){UserAccount user=patientUser(email);Appointment appointment=appointments.findOwned(appointmentId,email).filter(a->a.getPatient().getUser().getId().equals(user.getId())).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Rendez-vous introuvable."));if(appointment.getStatus()!=AppointmentStatus.COMPLETED)throw new ResponseStatusException(HttpStatus.CONFLICT,"Un avis est possible uniquement après une consultation terminée.");Patient patient=patients.findByUserId(user.getId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Profil patient introuvable."));Review review=reviews.findByAppointmentId(appointmentId).orElse(null);boolean created=review==null;if(created)review=reviews.save(new Review(appointment,patient,appointment.getDoctor(),request.rating(),request.recommend(),clean(request.comment())));else{if(!review.getPatient().getUser().getId().equals(user.getId()))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Avis introuvable.");review.update(request.rating(),request.recommend(),clean(request.comment()));}if(created)notifications.create(appointment.getDoctor().getUser(),appointment,"REVIEW_RECEIVED","Nouvel avis patient",user.getFirstName()+" "+user.getLastName()+" a laissé un avis après sa consultation.");return ReviewDto.from(review);}
 @Transactional(readOnly=true) public PagedResponse<ReviewDto> doctorReviews(String email,int page,int size){Doctor doctor=doctorProfiles.own(email);var found=reviews.findByDoctor(doctor.getId(),PageRequest.of(Math.max(0,page),Math.max(1,Math.min(size,100))));return PagedResponse.from(found.map(ReviewDto::from));}
 private UserAccount patientUser(String email){UserAccount user=users.findByEmailIgnoreCase(email).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentification requise."));if(!"ACTIVE".equals(user.getStatus())||(user.getRole()!=UserRole.USER&&user.getRole()!=UserRole.PATIENT))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Un compte patient actif est requis.");return user;}
 private String clean(String value){return value==null||value.isBlank()?null:value.trim();}
}
