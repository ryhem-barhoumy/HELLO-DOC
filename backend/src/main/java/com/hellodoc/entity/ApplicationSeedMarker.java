package com.hellodoc.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "application_seed_markers")
public class ApplicationSeedMarker {
    @Id @Column(length = 80) private String id;
    protected ApplicationSeedMarker() { }
    public ApplicationSeedMarker(String id) { this.id = id; }
    public String getId() { return id; }
}
