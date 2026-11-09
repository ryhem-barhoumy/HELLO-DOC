package com.hellodoc.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name="doctor_absences", indexes={@Index(name="ix_absence_doctor_dates", columnList="doctor_id,start_date,end_date")})
public class Absence {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="doctor_id", nullable=false) private Doctor doctor;
    @Column(name="start_date", nullable=false) private LocalDate startDate;
    @Column(name="end_date", nullable=false) private LocalDate endDate;
    @Column(name="start_time") private LocalTime startTime;
    @Column(name="end_time") private LocalTime endTime;
    @Column(nullable=false, length=120) private String reason;
    @Column(length=500) private String comment;
    protected Absence() { }
    /** Backward-compatible constructor for legacy all-day absences. */
    public Absence(Doctor doctor,LocalDate startDate,LocalDate endDate,String reason,String comment){this(doctor,startDate,endDate,null,null,reason,comment);}
    public Absence(Doctor doctor,LocalDate startDate,LocalDate endDate,LocalTime startTime,LocalTime endTime,String reason,String comment){this.doctor=doctor;this.update(startDate,endDate,startTime,endTime,reason,comment);}
    public void update(LocalDate start,LocalDate end,LocalTime from,LocalTime to,String reason,String comment){if(start==null||end==null||end.isBefore(start))throw new IllegalArgumentException("Période d’absence invalide.");if((from==null)!=(to==null)||from!=null&&!to.isAfter(from))throw new IllegalArgumentException("Pour une absence partielle, renseignez une heure de début et une fin postérieure.");if(from!=null&&!start.equals(end))throw new IllegalArgumentException("Une absence partielle doit concerner une seule date.");this.startDate=start;this.endDate=end;this.startTime=from;this.endTime=to;this.reason=reason;this.comment=comment;}
    public UUID getId(){return id;} public Doctor getDoctor(){return doctor;} public LocalDate getStartDate(){return startDate;} public LocalDate getEndDate(){return endDate;} public LocalTime getStartTime(){return startTime;} public LocalTime getEndTime(){return endTime;} public String getReason(){return reason;} public String getComment(){return comment;}
}
