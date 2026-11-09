package com.hellodoc.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="notifications",indexes={@Index(name="ix_notifications_user_created",columnList="recipient_user_id,created_at"),@Index(name="ix_notifications_user_read",columnList="recipient_user_id,read_at")})
public class Notification {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="recipient_user_id",nullable=false) private UserAccount recipient;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="appointment_id") private Appointment appointment;
    @Column(nullable=false,length=40) private String type;
    @Column(nullable=false,length=160) private String title;
    @Column(nullable=false,length=1000) private String message;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt=Instant.now();
    @Column(name="read_at") private Instant readAt;
    protected Notification() { }
    public Notification(UserAccount recipient,Appointment appointment,String type,String title,String message){this.recipient=recipient;this.appointment=appointment;this.type=type;this.title=title;this.message=message;}
    public void markRead(){if(readAt==null)readAt=Instant.now();}
    public UUID getId(){return id;} public UserAccount getRecipient(){return recipient;} public Appointment getAppointment(){return appointment;} public String getType(){return type;} public String getTitle(){return title;} public String getMessage(){return message;} public Instant getCreatedAt(){return createdAt;} public Instant getReadAt(){return readAt;}
}
