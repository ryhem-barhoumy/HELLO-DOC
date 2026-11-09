package com.hellodoc.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name="patients", uniqueConstraints=@UniqueConstraint(name="uk_patients_user", columnNames="user_id"), indexes=@Index(name="ix_patients_user", columnList="user_id"))
public class Patient {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @OneToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="user_id", nullable=false, unique=true) private UserAccount user;
    protected Patient() { }
    public Patient(UserAccount user) { this.user=user; }
    public UUID getId(){return id;} public UserAccount getUser(){return user;}
}
