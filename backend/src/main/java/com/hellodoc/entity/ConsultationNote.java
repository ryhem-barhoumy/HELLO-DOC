package com.hellodoc.entity;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name="consultation_notes",uniqueConstraints=@UniqueConstraint(name="uk_consultation_appointment",columnNames="appointment_id"))
public class ConsultationNote {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @OneToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="appointment_id",nullable=false,unique=true) private Appointment appointment;
    @Lob @Column(nullable=false,length=4000) private String resume="";
    @Lob @Column(nullable=false,length=8000) private String observation="";
    @Lob @Column(nullable=false,length=8000) private String treatment="";
    private LocalDate nextCheck;
    @Column(length=120) private String recommendedDelay;
    @Lob @Column(length=2000) private String followUpInstructions;
    @Lob @Column(nullable=false,length=4000) private String privateNote="";
    @Enumerated(EnumType.STRING) @Column(name="report_status",length=16) private ConsultationNoteStatus reportStatus=ConsultationNoteStatus.DRAFT;
    @Column(nullable=false,updatable=false) private Instant createdAt;
    @Column(nullable=false) private Instant updatedAt;
    @Version private long version;
    protected ConsultationNote(){}
    public ConsultationNote(Appointment appointment){this.appointment=appointment;this.createdAt=Instant.now();this.updatedAt=createdAt;}
    public void update(String resume,String observation,String treatment,LocalDate nextCheck,String privateNote){update(resume,observation,treatment,nextCheck,null,null,privateNote);}
    public void update(String resume,String observation,String treatment,LocalDate nextCheck,String recommendedDelay,String followUpInstructions,String privateNote){this.resume=clean(resume);this.observation=clean(observation);this.treatment=clean(treatment);this.nextCheck=nextCheck;this.recommendedDelay=clean(recommendedDelay);this.followUpInstructions=clean(followUpInstructions);this.privateNote=clean(privateNote);this.updatedAt=Instant.now();}
    public void finalizeReport(){this.reportStatus=ConsultationNoteStatus.FINALIZED;this.updatedAt=Instant.now();}
    public boolean isFinalized(){return (reportStatus==null?ConsultationNoteStatus.FINALIZED:reportStatus)==ConsultationNoteStatus.FINALIZED;}
    private String clean(String value){return value==null?"":value.trim();}
    public UUID getId(){return id;} public Appointment getAppointment(){return appointment;} public String getResume(){return resume;} public String getObservation(){return observation;} public String getTreatment(){return treatment;} public LocalDate getNextCheck(){return nextCheck;} public String getRecommendedDelay(){return recommendedDelay;} public String getFollowUpInstructions(){return followUpInstructions;} public String getPrivateNote(){return privateNote;} public ConsultationNoteStatus getReportStatus(){return reportStatus==null?ConsultationNoteStatus.FINALIZED:reportStatus;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;} public long getVersion(){return version;}
}
