package com.hellodoc.entity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="teleconsultation_messages",indexes=@Index(name="ix_teleconsultation_message_session_time",columnList="session_id,sent_at"))
public class TeleconsultationMessage {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="session_id",nullable=false) private TeleconsultationSession session;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="sender_id",nullable=false) private UserAccount sender;
 @Column(nullable=false,length=2000) private String body;
 @Column(name="sent_at",nullable=false,updatable=false) private Instant sentAt=Instant.now();
 protected TeleconsultationMessage(){}
 public TeleconsultationMessage(TeleconsultationSession session,UserAccount sender,String body){this.session=session;this.sender=sender;this.body=body.trim();}
 public UUID getId(){return id;} public UUID getSessionId(){return session.getId();} public String getSenderName(){return sender.getFirstName()+" "+sender.getLastName();} public Instant getSentAt(){return sentAt;} public String getBody(){return body;}
}
