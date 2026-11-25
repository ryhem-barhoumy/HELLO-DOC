package com.hellodoc.repository;

import com.hellodoc.entity.Appointment;
import com.hellodoc.entity.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment,UUID> {
 @Query("select a from Appointment a join fetch a.doctor d join fetch d.user join fetch a.patient p join fetch p.user where a.id=:id and lower(d.user.email)=lower(:email)")
 Optional<Appointment> findDoctorOwned(@Param("id") UUID id,@Param("email") String email);
 @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @Query("select a from Appointment a join fetch a.doctor d join fetch d.user join fetch a.patient p join fetch p.user where a.id=:id and lower(d.user.email)=lower(:email)")
 Optional<Appointment> lockDoctorOwned(@Param("id") UUID id,@Param("email") String email);
 @Query(value="select new com.hellodoc.dto.DoctorPatientSummary(a.patient.id,concat(a.patient.user.firstName,' ',a.patient.user.lastName),a.patient.user.phone,a.patient.user.email,count(a),max(a.startAt)) from Appointment a where a.doctor.id=:doctorId and a.status=com.hellodoc.entity.AppointmentStatus.COMPLETED and (:q is null or lower(concat(a.patient.user.firstName,' ',a.patient.user.lastName)) like :q) group by a.patient.id,a.patient.user.firstName,a.patient.user.lastName,a.patient.user.phone,a.patient.user.email order by max(a.startAt) desc",countQuery="select count(distinct a.patient.id) from Appointment a where a.doctor.id=:doctorId and a.status=com.hellodoc.entity.AppointmentStatus.COMPLETED and (:q is null or lower(concat(a.patient.user.firstName,' ',a.patient.user.lastName)) like :q)")
 Page<com.hellodoc.dto.DoctorPatientSummary> findCompletedPatients(@Param("doctorId") UUID doctorId,@Param("q") String q,Pageable pageable);
 long countByDoctorIdAndPatientIdAndStatus(UUID doctorId,UUID patientId,AppointmentStatus status);
 long countByPatientId(UUID patientId);
 @Query("select a from Appointment a join fetch a.doctor d join fetch d.user where a.patient.user.id=:patientUserId and a.status=com.hellodoc.entity.AppointmentStatus.COMPLETED order by a.startAt desc")
 List<Appointment> findPatientCompletedByUserId(@Param("patientUserId") UUID patientUserId);
 long countByDoctorId(UUID doctorId);
 long countByStatus(AppointmentStatus status);
 long countByStartAtGreaterThanEqualAndStartAtLessThan(Instant from,Instant to);
 @Query("select a from Appointment a where (:status is null or a.status=:status) and (:from is null or a.startAt>=:from) and (:to is null or a.startAt<:to) and (:q is null or lower(concat(a.patient.user.firstName,' ',a.patient.user.lastName,' ',a.doctor.user.firstName,' ',a.doctor.user.lastName)) like :q) order by a.startAt desc")
 Page<Appointment> findOperational(@Param("status") AppointmentStatus status,@Param("from") Instant from,@Param("to") Instant to,@Param("q") String q,Pageable pageable);
 @Query("select a from Appointment a where a.doctor.id=:doctorId and a.patient.id=:patientId and a.status=com.hellodoc.entity.AppointmentStatus.COMPLETED order by a.startAt desc")
 Page<Appointment> findCompletedVisits(@Param("doctorId") UUID doctorId,@Param("patientId") UUID patientId,Pageable pageable);
 @Query("select a from Appointment a where lower(a.patient.user.email)=lower(:email) and ((:history=false and (a.status=com.hellodoc.entity.AppointmentStatus.TO_RESCHEDULE or (a.startAt>=:now and a.status in :active))) or (:history=true and a.status<>com.hellodoc.entity.AppointmentStatus.TO_RESCHEDULE and (a.startAt<:now or a.status in :finished))) order by a.startAt desc")
 Page<Appointment> findPatientAppointments(@Param("email") String email,@Param("history") boolean history,@Param("now") Instant now,@Param("active") List<AppointmentStatus> active,@Param("finished") List<AppointmentStatus> finished,Pageable pageable);
 @Query("select a from Appointment a where lower(a.patient.user.email)=lower(:email) and a.startAt>=:now and a.status in :active order by a.startAt asc")
 List<Appointment> findNextPatientAppointments(@Param("email") String email,@Param("now") Instant now,@Param("active") List<AppointmentStatus> active,Pageable pageable);
 @Query("select a from Appointment a where a.doctor.id=:doctorId and (:from is null or a.startAt>=:from) and (:to is null or a.startAt<:to) order by a.startAt asc")
 Page<Appointment> findDoctorAppointments(@Param("doctorId") UUID doctorId,@Param("from") Instant from,@Param("to") Instant to,Pageable pageable);
 @Query("select a from Appointment a where a.doctor.id=:doctorId and a.startAt>=:now and a.status in :statuses order by a.startAt asc")
 List<Appointment> findUpcomingDoctorAppointments(@Param("doctorId") UUID doctorId,@Param("now") Instant now,@Param("statuses") List<AppointmentStatus> statuses,Pageable pageable);
 @Query("select a from Appointment a where a.doctor.id=:doctorId and a.status in :statuses and a.startAt<:to and a.endAt>:from order by a.startAt")
 List<Appointment> findOverlapping(@Param("doctorId") UUID doctorId,@Param("statuses") List<AppointmentStatus> statuses,@Param("from") Instant from,@Param("to") Instant to);
 @Query("select a from Appointment a where a.id=:id and (lower(a.patient.user.email)=lower(:email) or lower(a.doctor.user.email)=lower(:email))")
 Optional<Appointment> findOwned(@Param("id") UUID id,@Param("email") String email);
 @Query("select count(a) from Appointment a where a.patient.user.id=:userId and (a.status=com.hellodoc.entity.AppointmentStatus.TO_RESCHEDULE or (a.startAt>=:now and a.status in :statuses))") long countPatientUpcoming(@Param("userId") UUID userId,@Param("now") Instant now,@Param("statuses") List<AppointmentStatus> statuses);
 @Query("select count(a) from Appointment a where a.patient.user.id=:userId and a.status<>com.hellodoc.entity.AppointmentStatus.TO_RESCHEDULE and (a.startAt<:now or a.status in :finished)") long countPatientPast(@Param("userId") UUID userId,@Param("now") Instant now,@Param("finished") List<AppointmentStatus> finished);
 long countByDoctorIdAndStatusIn(UUID doctorId,List<AppointmentStatus> statuses);
 long countByDoctorIdAndStatus(UUID doctorId,AppointmentStatus status);
 @Query("select count(a) from Appointment a where a.doctor.id=:doctorId and a.status=:status and a.startAt>=:now") long countDoctorUpcomingStatus(@Param("doctorId") UUID doctorId,@Param("status") AppointmentStatus status,@Param("now") Instant now);
 @Query("select count(a) from Appointment a where a.doctor.id=:doctorId and a.startAt>=:start and a.startAt<:end and a.status in :statuses") long countDoctorToday(@Param("doctorId") UUID doctorId,@Param("start") Instant start,@Param("end") Instant end,@Param("statuses") List<AppointmentStatus> statuses);
 @Query("select count(a) from Appointment a where a.doctor.id=:doctorId and a.startAt>=:start and a.startAt<:end") long countDoctorBetween(@Param("doctorId") UUID doctorId,@Param("start") Instant start,@Param("end") Instant end);
}
