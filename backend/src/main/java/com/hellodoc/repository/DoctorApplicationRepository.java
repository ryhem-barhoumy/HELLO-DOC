package com.hellodoc.repository;

import com.hellodoc.entity.DoctorApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface DoctorApplicationRepository extends JpaRepository<DoctorApplication,UUID> {
 long countByStatus(String status);
 List<DoctorApplication> findByStatusOrderByIdDesc(String status);
}
