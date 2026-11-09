package com.hellodoc.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="appointment_reviews",uniqueConstraints=@UniqueConstraint(name="uk_review_appointment",columnNames="appointment_id"),indexes=@Index(name="ix_review_doctor_created",columnList="doctor_id,created_at"))
public class Review {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @OneToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="appointment_id",nullable=false) private Appointment appointment;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="patient_id",nullable=false) private Patient patient;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="doctor_id",nullable=false) private Doctor doctor;
    @Column(nullable=false) private int rating;
    @Column(nullable=false,length=3) private String recommend;
    @Column(length=2000) private String comment;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt=Instant.now();
    @Column(name="updated_at",nullable=false) private Instant updatedAt=Instant.now();
    protected Review() { }
    public Review(Appointment appointment,Patient patient,Doctor doctor,int rating,String recommend,String comment){this.appointment=appointment;this.patient=patient;this.doctor=doctor;update(rating,recommend,comment);}
    public void update(int rating,String recommend,String comment){this.rating=rating;this.recommend=recommend;this.comment=comment;this.updatedAt=Instant.now();}
    public UUID getId(){return id;} public Appointment getAppointment(){return appointment;} public Patient getPatient(){return patient;} public Doctor getDoctor(){return doctor;} public int getRating(){return rating;} public String getRecommend(){return recommend;} public String getComment(){return comment;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
