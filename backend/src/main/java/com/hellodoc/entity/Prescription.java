package com.hellodoc.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name="prescriptions", indexes=@Index(name="ix_prescription_appointment", columnList="appointment_id"))
public class Prescription {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="appointment_id", nullable=false) private Appointment appointment;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="supersedes_id") private Prescription supersedes;
    @Column(nullable=false, updatable=false) private Instant prescribedAt;
    @Column(nullable=false, length=4000) private String recommendations="";
    @Column(nullable=false) private boolean amended;
    @Enumerated(EnumType.STRING) @Column(name="prescription_status",length=16) private PrescriptionStatus status=PrescriptionStatus.DRAFT;
    @OneToMany(mappedBy="prescription", cascade=CascadeType.ALL, orphanRemoval=true)
    @OrderColumn(name="line_order") private List<PrescriptionItem> items=new ArrayList<>();
    protected Prescription() {}
    public Prescription(Appointment appointment, String recommendations, List<PrescriptionItem> items, Prescription supersedes) {
        this.appointment=appointment; this.recommendations=clean(recommendations); this.prescribedAt=Instant.now(); this.supersedes=supersedes; this.amended=supersedes!=null;
        for (PrescriptionItem item:items) { item.attach(this); this.items.add(item); }
    }
    private String clean(String s){return s==null?"":s.trim();}
    public void finalizePrescription(){status=PrescriptionStatus.FINALIZED;}
    public boolean isFinalized(){return (status==null?PrescriptionStatus.FINALIZED:status)==PrescriptionStatus.FINALIZED;}
    public UUID getId(){return id;} public Appointment getAppointment(){return appointment;} public Prescription getSupersedes(){return supersedes;} public Instant getPrescribedAt(){return prescribedAt;}
    public String getRecommendations(){return recommendations;} public boolean isAmended(){return amended;} public PrescriptionStatus getStatus(){return status==null?PrescriptionStatus.FINALIZED:status;} public List<PrescriptionItem> getItems(){return List.copyOf(items);}
}
