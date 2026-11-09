package com.hellodoc.repository;
import com.hellodoc.entity.Absence; import org.springframework.data.jpa.repository.JpaRepository; import java.time.LocalDate; import java.util.*;
public interface AbsenceRepository extends JpaRepository<Absence,UUID> { List<Absence> findByDoctorIdOrderByStartDateAsc(UUID doctorId); List<Absence> findByDoctorIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(UUID doctorId,LocalDate end,LocalDate start); Optional<Absence> findByIdAndDoctorId(UUID id,UUID doctorId); void deleteByDoctorId(UUID doctorId); }
