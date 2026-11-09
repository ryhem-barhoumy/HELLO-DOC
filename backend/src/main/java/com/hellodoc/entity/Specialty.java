package com.hellodoc.entity;

import jakarta.persistence.*;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name="specialties", uniqueConstraints=@UniqueConstraint(name="uk_specialties_key", columnNames="normalized_name"), indexes=@Index(name="ix_specialties_name", columnList="name"))
public class Specialty {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(nullable=false, length=120) private String name;
    @Column(name="normalized_name", nullable=false, length=120) private String normalizedName;
    protected Specialty() { }
    public Specialty(String name) { rename(name); }
    public static String normalize(String value) { String clean=value == null ? "" : value.trim().replaceAll("\\s+", " "); return Normalizer.normalize(clean, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).replaceAll("\\s+", " "); }
    public void rename(String value) {
        String clean=value == null ? "" : value.trim().replaceAll("\\s+", " ");
        this.name=clean;
        this.normalizedName=normalize(clean);
    }
    public UUID getId(){return id;} public String getName(){return name;} public String getNormalizedName(){return normalizedName;}
}
