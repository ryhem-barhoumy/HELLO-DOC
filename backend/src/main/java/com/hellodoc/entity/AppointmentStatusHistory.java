package com.hellodoc.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="appointment_status_history",indexes=@Index(name="ix_appointment_history_appointment_time",columnList="appointment_id,changed_at"))
public class AppointmentStatusHistory {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="appointment_id",nullable=false) private Appointment appointment;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="changed_by_user_id",nullable=false) private UserAccount changedBy;
    @Enumerated(EnumType.STRING) @Column(name="old_status",length=20) private AppointmentStatus oldStatus;
    @Enumerated(EnumType.STRING) @Column(name="new_status",nullable=false,length=20) private AppointmentStatus newStatus;
    @Column(name="changed_at",nullable=false) private Instant changedAt=Instant.now();
    @Column(length=500) private String reason;
    protected AppointmentStatusHistory() { }
    public AppointmentStatusHistory(Appointment appointment,UserAccount changedBy,AppointmentStatus oldStatus,AppointmentStatus newStatus,String reason){this.appointment=appointment;this.changedBy=changedBy;this.oldStatus=oldStatus;this.newStatus=newStatus;this.reason=reason;}
    public UUID getId(){return id;} public Appointment getAppointment(){return appointment;} public UserAccount getChangedBy(){return changedBy;} public AppointmentStatus getOldStatus(){return oldStatus;} public AppointmentStatus getNewStatus(){return newStatus;} public Instant getChangedAt(){return changedAt;} public String getReason(){return reason;}
}
