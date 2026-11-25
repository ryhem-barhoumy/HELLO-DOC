package com.hellodoc.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="teleconsultation_sessions", uniqueConstraints=@UniqueConstraint(name="uk_teleconsultation_appointment", columnNames="appointment_id"))
public class TeleconsultationSession {
    public enum State { WAITING, ACTIVE, COMPLETED }
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @OneToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="appointment_id",nullable=false) private Appointment appointment;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private State state=State.WAITING;
    @Column(nullable=false,updatable=false) private Instant createdAt=Instant.now();
    private Instant startedAt; private Instant completedAt;
    protected TeleconsultationSession(){}
    public TeleconsultationSession(Appointment appointment){this.appointment=appointment;}
    public void start(){if(state==State.COMPLETED)throw new IllegalStateException("A completed session cannot restart");if(state==State.WAITING){state=State.ACTIVE;startedAt=Instant.now();}}
    public void complete(){if(state==State.COMPLETED)return;state=State.COMPLETED;completedAt=Instant.now();}
    public UUID getId(){return id;} public Appointment getAppointment(){return appointment;} public State getState(){return state;} public Instant getCreatedAt(){return createdAt;} public Instant getStartedAt(){return startedAt;} public Instant getCompletedAt(){return completedAt;}
}
