package com.hellodoc.repository;

import com.hellodoc.entity.TeleconsultationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface TeleconsultationSessionRepository extends JpaRepository<TeleconsultationSession,UUID> {
    Optional<TeleconsultationSession> findByAppointmentId(UUID appointmentId);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a join fetch a.doctor d join fetch d.user join fetch a.patient p join fetch p.user where a.id=:id and lower(d.user.email)=lower(:email)")
    Optional<com.hellodoc.entity.Appointment> lockDoctorAppointment(@Param("id") UUID appointmentId,@Param("email") String email);
    @Query("select s from TeleconsultationSession s join fetch s.appointment a join fetch a.doctor d join fetch d.user join fetch a.patient p join fetch p.user where s.id=:id")
    Optional<TeleconsultationSession> findParticipantContext(@Param("id") UUID id);
}
