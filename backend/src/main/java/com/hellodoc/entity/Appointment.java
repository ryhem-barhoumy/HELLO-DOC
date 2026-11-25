package com.hellodoc.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="appointments", uniqueConstraints=@UniqueConstraint(name="uk_appointment_doctor_reservation_key", columnNames={"doctor_id","reservation_key"}), indexes={@Index(name="ix_appointment_patient_start",columnList="patient_id,start_at"),@Index(name="ix_appointment_doctor_status_start",columnList="doctor_id,status,start_at")})
public class Appointment {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="doctor_id",nullable=false) private Doctor doctor;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="patient_id",nullable=false) private Patient patient;
    @Column(name="service_name",nullable=false,length=160) private String serviceName;
    @Column(name="service_price",length=80) private String servicePrice;
    @Enumerated(EnumType.STRING) @Column(name="consultation_mode",length=16) private ConsultationMode consultationMode;
    @Column(name="start_at",nullable=false) private Instant startAt;
    @Column(name="end_at",nullable=false) private Instant endAt;
    @Column(name="reservation_key") private Instant reservationKey;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private AppointmentStatus status;
    @Column(length=500) private String reason;
    @Column(name="cancellation_reason",length=500) private String cancellationReason;
    @Column(name="meet_link",length=500) private String meetLink;
    @Column(name="meet_call_ended_at") private Instant meetCallEndedAt;
    @Column(name="meet_call_ended_by",length=255) private String meetCallEndedBy;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    @Version private long version;
    protected Appointment() { }
    public Appointment(Doctor doctor,Patient patient,String serviceName,String servicePrice,Instant startAt,Instant endAt,String reason){this(doctor,patient,serviceName,servicePrice,startAt,endAt,reason,ConsultationMode.IN_PERSON);}
    public Appointment(Doctor doctor,Patient patient,String serviceName,String servicePrice,Instant startAt,Instant endAt,String reason,ConsultationMode consultationMode){this.doctor=doctor;this.patient=patient;this.serviceName=serviceName;this.servicePrice=servicePrice;this.startAt=startAt;this.endAt=endAt;this.reservationKey=startAt;this.reason=reason;this.consultationMode=consultationMode==null?ConsultationMode.IN_PERSON:consultationMode;this.status=AppointmentStatus.PENDING;this.createdAt=Instant.now();}
    public void changeStatus(AppointmentStatus status,String reason){this.status=status;if(status==AppointmentStatus.CANCELLED||status==AppointmentStatus.REJECTED||status==AppointmentStatus.TO_RESCHEDULE)this.reservationKey=null;if(status==AppointmentStatus.CANCELLED)this.cancellationReason=reason;}
    public void setMeetLink(String meetLink){if(!java.util.Objects.equals(this.meetLink,meetLink)){this.meetCallEndedAt=null;this.meetCallEndedBy=null;}this.meetLink=meetLink;}
    public void markMeetCallEnded(String doctorEmail){if(meetLink==null||meetLink.isBlank())throw new IllegalStateException("A Google Meet link is required before ending the external call.");if(meetCallEndedAt==null){meetCallEndedAt=Instant.now();meetCallEndedBy=doctorEmail;}}
    public void reschedule(Instant startAt,Instant endAt,String reason){this.startAt=startAt;this.endAt=endAt;this.reservationKey=startAt;this.status=AppointmentStatus.PENDING;if(reason!=null&&!reason.isBlank())this.reason=reason.trim();}
    public UUID getId(){return id;} public Doctor getDoctor(){return doctor;} public Patient getPatient(){return patient;} public String getServiceName(){return serviceName;} public String getServicePrice(){return servicePrice;} public Instant getStartAt(){return startAt;} public Instant getEndAt(){return endAt;} public AppointmentStatus getStatus(){return status;} public ConsultationMode getConsultationMode(){return consultationMode==null?ConsultationMode.IN_PERSON:consultationMode;} public String getReason(){return reason;} public String getCancellationReason(){return cancellationReason;} public String getMeetLink(){return meetLink;} public Instant getMeetCallEndedAt(){return meetCallEndedAt;} public String getMeetCallEndedBy(){return meetCallEndedBy;} public Instant getCreatedAt(){return createdAt;}
}
