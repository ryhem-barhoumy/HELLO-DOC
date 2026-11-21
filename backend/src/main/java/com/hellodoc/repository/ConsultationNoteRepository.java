package com.hellodoc.repository;
import com.hellodoc.entity.ConsultationNote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ConsultationNoteRepository extends JpaRepository<ConsultationNote,UUID> {
    Optional<ConsultationNote> findByAppointmentId(UUID appointmentId);
    List<ConsultationNote> findByAppointmentIdIn(Collection<UUID> appointmentIds);
}
