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
    protected ConsultationRevision(){}
    public ConsultationRevision(ConsultationNote note,String editorName,String action){this.note=note;this.editorName=editorName;this.action=action;this.noteVersion=note.getVersion();this.changedAt=Instant.now();}
    public String getEditorName(){return editorName;} public String getAction(){return action;} public long getNoteVersion(){return noteVersion;} public Instant getChangedAt(){return changedAt;}
}
