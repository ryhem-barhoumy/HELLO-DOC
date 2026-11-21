package com.hellodoc.entity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="administrative_activities",indexes=@Index(name="ix_admin_activity_time",columnList="occurred_at"))
public class AdministrativeActivity {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(nullable=false,length=254,updatable=false) private String actorName;
    @Column(nullable=false,length=50,updatable=false) private String action;
    @Column(nullable=false,length=30,updatable=false) private String targetType;
    @Column(nullable=false,updatable=false) private UUID targetId;
    @Column(name="occurred_at",nullable=false,updatable=false) private Instant occurredAt;
    protected AdministrativeActivity(){}
    public AdministrativeActivity(String actorName,String action,String targetType,UUID targetId){this.actorName=actorName;this.action=action;this.targetType=targetType;this.targetId=targetId;this.occurredAt=Instant.now();}
    public UUID getId(){return id;} public String getActorName(){return actorName;} public String getAction(){return action;} public String getTargetType(){return targetType;} public UUID getTargetId(){return targetId;} public Instant getOccurredAt(){return occurredAt;}
}
