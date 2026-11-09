package com.hellodoc.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Executes child-to-parent deletes immediately so MySQL foreign keys are respected. */
@Repository
public class AdminAccountDeletionRepository {
    @PersistenceContext private EntityManager entityManager;

    @Transactional public int deleteAbsences(UUID doctorId) { return delete("delete from doctor_absences where doctor_id = :id", doctorId); }
    @Transactional public int deleteAvailabilities(UUID doctorId) { return delete("delete from availabilities where doctor_id = :id", doctorId); }
    @Transactional public int deleteDoctorSpecialties(UUID doctorId) { return delete("delete from doctor_specialties where doctor_id = :id", doctorId); }
    @Transactional public int deleteDoctor(UUID doctorId) { return delete("delete from doctors where id = :id", doctorId); }
    @Transactional public int deleteDoctorApplication(UUID applicationId) { return delete("delete from doctor_applications where id = :id", applicationId); }
    @Transactional public int deletePatient(UUID patientId) { return delete("delete from patients where id = :id", patientId); }
    @Transactional public int deleteUser(UUID userId) { return delete("delete from users where id = :id", userId); }

    private int delete(String sql, UUID id) {
        return entityManager.createNativeQuery(sql).setParameter("id", id).executeUpdate();
    }
}
