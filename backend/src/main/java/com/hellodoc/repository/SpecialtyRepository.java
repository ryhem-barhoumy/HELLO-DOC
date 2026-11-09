package com.hellodoc.repository;
import com.hellodoc.entity.Specialty; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface SpecialtyRepository extends JpaRepository<Specialty,UUID> { Optional<Specialty> findByNormalizedName(String normalizedName); List<Specialty> findAllByOrderByNameAsc(); }
