package com.hellodoc.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name="prescription_items")
public class PrescriptionItem {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="prescription_id", nullable=false) private Prescription prescription;
    @Column(nullable=false, length=300) private String medication;
    @Column(nullable=false, length=1000) private String instructions;
    protected PrescriptionItem() {}
    public PrescriptionItem(String medication, String instructions){this.medication=medication.trim();this.instructions=instructions.trim();}
    void attach(Prescription prescription){this.prescription=prescription;}
    public UUID getId(){return id;} public String getMedication(){return medication;} public String getInstructions(){return instructions;}
}
