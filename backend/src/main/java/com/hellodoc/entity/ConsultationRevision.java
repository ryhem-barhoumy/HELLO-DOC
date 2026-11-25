package com.hellodoc.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Modification metadata only: clinical text is not copied into an operational log. */
@Entity
@Table(name="consultation_revisions",indexes=@Index(name="ix_consultation_revision_note",columnList="note_id,changed_at"))
public class ConsultationRevision {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="note_id",nullable=false) private ConsultationNote note;
    @Column(nullable=false,length=220) private String editorName;
    @Column(nullable=false,length=16) private String action;
    @Column(nullable=false) private long noteVersion;
    @Column(name="changed_at",nullable=false,updatable=false) private Instant changedAt;
    @Column(name="correction_reason",length=500) private String correctionReason;
    @Lob @Column(name="previous_resume") private String previousResume;
    @Lob @Column(name="previous_observation") private String previousObservation;
    @Lob @Column(name="previous_treatment") private String previousTreatment;
    @Column(name="previous_next_check") private java.time.LocalDate previousNextCheck;
    @Column(name="previous_recommended_delay",length=120) private String previousRecommendedDelay;
    @Lob @Column(name="previous_follow_up_instructions") private String previousFollowUpInstructions;
    @Lob @Column(name="previous_private_note") private String previousPrivateNote;
    protected ConsultationRevision(){}
    public ConsultationRevision(ConsultationNote note,String editorName,String action){this.note=note;this.editorName=editorName;this.action=action;this.noteVersion=note.getVersion();this.changedAt=Instant.now();}
    public ConsultationRevision(ConsultationNote note,String editorName,String action,String correctionReason,long previousVersion,String previousResume,String previousObservation,String previousTreatment,java.time.LocalDate previousNextCheck,String previousRecommendedDelay,String previousFollowUpInstructions,String previousPrivateNote){this(note,editorName,action);this.noteVersion=previousVersion;this.correctionReason=correctionReason;this.previousResume=previousResume;this.previousObservation=previousObservation;this.previousTreatment=previousTreatment;this.previousNextCheck=previousNextCheck;this.previousRecommendedDelay=previousRecommendedDelay;this.previousFollowUpInstructions=previousFollowUpInstructions;this.previousPrivateNote=previousPrivateNote;}
    public String getEditorName(){return editorName;} public String getAction(){return action;} public long getNoteVersion(){return noteVersion;} public Instant getChangedAt(){return changedAt;} public String getCorrectionReason(){return correctionReason;} public String getPreviousResume(){return previousResume;} public String getPreviousObservation(){return previousObservation;} public String getPreviousTreatment(){return previousTreatment;} public java.time.LocalDate getPreviousNextCheck(){return previousNextCheck;} public String getPreviousRecommendedDelay(){return previousRecommendedDelay;} public String getPreviousFollowUpInstructions(){return previousFollowUpInstructions;} public String getPreviousPrivateNote(){return previousPrivateNote;}
}
