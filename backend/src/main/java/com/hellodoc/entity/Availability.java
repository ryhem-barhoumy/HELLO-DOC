package com.hellodoc.entity;

import jakarta.persistence.*;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name="availabilities", uniqueConstraints=@UniqueConstraint(name="uk_availability_weekday_window", columnNames={"doctor_id","iso_weekday","start_time","end_time"}), indexes=@Index(name="ix_availability_doctor_day", columnList="doctor_id,iso_weekday"))
public class Availability {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(optional=false, fetch=FetchType.LAZY) @JoinColumn(name="doctor_id", nullable=false) private Doctor doctor;
    @Column(name="iso_weekday", nullable=false) private int isoWeekday;
    @Column(name="window_number",nullable=false,columnDefinition="integer default 1") private int windowNumber=1;
    @Column(name="start_time", nullable=false) private LocalTime startTime;
    @Column(name="end_time", nullable=false) private LocalTime endTime;
    @Column(name="slot_duration_minutes", nullable=false) private int slotDurationMinutes;
    @Column(name="pause_minutes", nullable=false) private int pauseMinutes;
    protected Availability() { }
    public Availability(Doctor doctor,int isoWeekday,LocalTime startTime,LocalTime endTime,int slotDurationMinutes,int pauseMinutes){this(doctor,isoWeekday,1,startTime,endTime,slotDurationMinutes,pauseMinutes);}
    public Availability(Doctor doctor,int isoWeekday,int windowNumber,LocalTime startTime,LocalTime endTime,int slotDurationMinutes,int pauseMinutes){this.doctor=doctor;this.windowNumber=windowNumber;this.update(isoWeekday,startTime,endTime,slotDurationMinutes,pauseMinutes);}
    public void update(int day,LocalTime start,LocalTime end,int duration,int pause){if(day<1||day>7||start==null||end==null||!end.isAfter(start)||start.getMinute()%5!=0||end.getMinute()%5!=0||start.getSecond()!=0||end.getSecond()!=0||duration<5||duration>240||pause<0||pause>240||java.time.Duration.between(start,end).toMinutes()<duration)throw new IllegalArgumentException("Plage invalide : heures par pas de 5 minutes, fin après début et au moins un créneau complet.");this.isoWeekday=day;this.startTime=start;this.endTime=end;this.slotDurationMinutes=duration;this.pauseMinutes=pause;}
    public UUID getId(){return id;} public Doctor getDoctor(){return doctor;} public int getIsoWeekday(){return isoWeekday;} public int getWindowNumber(){return windowNumber;} public LocalTime getStartTime(){return startTime;} public LocalTime getEndTime(){return endTime;} public int getSlotDurationMinutes(){return slotDurationMinutes;} public int getPauseMinutes(){return pauseMinutes;}
}
