package com.hellodoc.repository;

import com.hellodoc.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {
    List<Prescription> findByAppointmentIdInOrderByPrescribedAtDesc(List<UUID> appointmentIds);
}
