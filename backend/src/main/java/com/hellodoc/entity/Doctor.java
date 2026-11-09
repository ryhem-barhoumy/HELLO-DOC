package com.hellodoc.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name="doctors", uniqueConstraints={@UniqueConstraint(name="uk_doctors_user", columnNames="user_id"), @UniqueConstraint(name="uk_doctors_application", columnNames="application_id")}, indexes={@Index(name="ix_doctors_city", columnList="city"), @Index(name="ix_doctors_active", columnList="active")})
public class Doctor {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @OneToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="user_id", nullable=false, unique=true) private UserAccount user;
    @OneToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="application_id", nullable=false, unique=true) private DoctorApplication application;
    @ManyToMany(fetch=FetchType.LAZY) @JoinTable(name="doctor_specialties", joinColumns=@JoinColumn(name="doctor_id"), inverseJoinColumns=@JoinColumn(name="specialty_id"), uniqueConstraints=@UniqueConstraint(name="uk_doctor_specialty", columnNames={"doctor_id","specialty_id"}), indexes={@Index(name="ix_doctor_specialties_specialty", columnList="specialty_id")})
    private Set<Specialty> specialties=new HashSet<>();
    @Column(name="service_title", nullable=false, length=160) private String serviceTitle;
    @Column(nullable=false, length=120) private String city;
    @Column(nullable=false, length=300) private String address;
    @Column(nullable=false, length=300) private String location;
    @Column(nullable=false, length=2000) private String description;
    @Column(name="duration_minutes", nullable=false) private int durationMinutes;
    @Column(name="pause_minutes", nullable=false) private int pauseMinutes;
    @Lob @Column(name="services_json", nullable=false) private String servicesJson="[]";
    @Column(nullable=false) private boolean active=true;
    protected Doctor() { }
    public Doctor(UserAccount user,DoctorApplication application,String serviceTitle,String city,String address,String location,String description,int durationMinutes,int pauseMinutes,String servicesJson){this.user=user;this.application=application;this.serviceTitle=serviceTitle;this.city=city;this.address=address;this.location=location;this.description=description;this.durationMinutes=durationMinutes;this.pauseMinutes=pauseMinutes;this.servicesJson=servicesJson;if(application!=null)application.attachDoctor(this);}
    public void addSpecialty(Specialty specialty){specialties.add(specialty);}
    public void setActive(boolean active){this.active=active;}
    public void updateProfile(String serviceTitle,String city,String address,String location,String description,int durationMinutes,int pauseMinutes,String servicesJson){this.serviceTitle=serviceTitle;this.city=city;this.address=address;this.location=location;this.description=description;this.durationMinutes=durationMinutes;this.pauseMinutes=pauseMinutes;this.servicesJson=servicesJson;}
    public UUID getId(){return id;} public UserAccount getUser(){return user;} public DoctorApplication getApplication(){return application;} public Set<Specialty> getSpecialties(){return specialties;} public String getServiceTitle(){return serviceTitle;} public String getCity(){return city;} public String getAddress(){return address;} public String getLocation(){return location;} public String getDescription(){return description;} public int getDurationMinutes(){return durationMinutes;} public int getPauseMinutes(){return pauseMinutes;} public String getServicesJson(){return servicesJson;} public boolean isActive(){return active;}
}
