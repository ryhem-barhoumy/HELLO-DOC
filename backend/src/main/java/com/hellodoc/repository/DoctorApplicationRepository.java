package com.hellodoc.repository;

import com.hellodoc.entity.DoctorApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DoctorApplicationRepository extends JpaRepository<DoctorApplication,UUID> {
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select a from DoctorApplication a join fetch a.user where a.id=:id")
 Optional<DoctorApplication> findByIdForUpdate(@Param("id") UUID id);
 long countByStatus(String status);
 boolean existsByUser_Id(UUID userId);
 List<DoctorApplication> findByStatusOrderByIdDesc(String status);
}
