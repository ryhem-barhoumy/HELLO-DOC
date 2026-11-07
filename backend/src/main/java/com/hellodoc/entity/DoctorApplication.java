package com.hellodoc.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name="doctor_applications", uniqueConstraints=@UniqueConstraint(name="uk_doctor_application_user",columnNames="user_id"))
public class DoctorApplication {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @OneToOne(optional=false) @JoinColumn(name="user_id",nullable=false,unique=true) private UserAccount user;
 @Column(nullable=false,length=160) private String serviceTitle;
 @Column(nullable=false,length=120) private String specialty;
 @Column(nullable=false,length=100) private String licenseNumber;
 @Column(nullable=false,length=120) private String city;
 @Column(nullable=false,length=300) private String practiceAddress;
 @Column(nullable=false,length=20) private String status;
 @Column(nullable=false) private int durationMinutes;
 @Column(nullable=false,length=10) private String workStart;
 @Column(nullable=false,length=10) private String workEnd;
 @Column(nullable=false) private int pauseMinutes;
 @Lob @Column(nullable=false) private String workingDaysJson;
 @Lob @Column(nullable=false) private String servicesJson;
 @Lob @Column(nullable=false) private String description;
 @Column(length=1000) private String decisionReason;
 protected DoctorApplication() { }
 public DoctorApplication(UserAccount user,String serviceTitle,String specialty,String licenseNumber,String city,String practiceAddress,int durationMinutes,String workStart,String workEnd,int pauseMinutes,String workingDaysJson,String servicesJson,String description){
  this.user=user;this.serviceTitle=serviceTitle;this.specialty=specialty;this.licenseNumber=licenseNumber;this.city=city;this.practiceAddress=practiceAddress;this.durationMinutes=durationMinutes;this.workStart=workStart;this.workEnd=workEnd;this.pauseMinutes=pauseMinutes;this.workingDaysJson=workingDaysJson;this.servicesJson=servicesJson;this.description=description;this.status="PENDING";
 }
 public UUID getId(){return id;} public UserAccount getUser(){return user;} public String getServiceTitle(){return serviceTitle;} public String getSpecialty(){return specialty;}
 public String getLicenseNumber(){return licenseNumber;} public String getCity(){return city;} public String getPracticeAddress(){return practiceAddress;} public String getStatus(){return status;}
 public String getDescription(){return description;} public void decide(String status,String reason){this.status=status;this.decisionReason=reason;}
}
