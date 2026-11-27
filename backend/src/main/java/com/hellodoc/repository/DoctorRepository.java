package com.hellodoc.repository;
import com.hellodoc.entity.Doctor; import jakarta.persistence.LockModeType; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.util.*;
public interface DoctorRepository extends JpaRepository<Doctor,UUID> {
 @EntityGraph(attributePaths={"user","application"}) Page<Doctor> findByApplicationApprovalEmailStatusOrderByIdDesc(String status,Pageable pageable);
 @Query("select count(d) from Doctor d join d.specialties s where s.id=:specialtyId") long countDoctorsBySpecialtyId(@Param("specialtyId") UUID specialtyId);
 @Override @EntityGraph(attributePaths={"user","application"}) Page<Doctor> findAll(Pageable pageable);
 Optional<Doctor> findByUserEmailIgnoreCase(String email);
 String DIRECTORY_FILTER = """
         from Doctor d join d.user u left join d.specialties s
         where d.active = true and u.status = 'ACTIVE' and u.role = com.hellodoc.entity.UserRole.DOCTOR
         and (:q is null or lower(concat(u.firstName, ' ', u.lastName)) like lower(concat('%', :q, '%'))
              or lower(d.serviceTitle) like lower(concat('%', :q, '%'))
              or (:includePrivateAddress = true and lower(d.address) like lower(concat('%', :q, '%'))))
         and (:city is null or lower(d.city) like lower(concat('%', :city, '%'))
              or (:includePrivateAddress = true and lower(concat(d.city, ' ', d.address)) like lower(concat('%', :city, '%'))))
         and (:specialty is null or s.normalizedName = :specialty)
         """;
 @Query(value="select distinct d " + DIRECTORY_FILTER, countQuery="select count(distinct d.id) " + DIRECTORY_FILTER)
 Page<Doctor> search(@Param("q") String q,@Param("city") String city,@Param("specialty") String specialty,
                     @Param("includePrivateAddress") boolean includePrivateAddress, Pageable pageable);
 Optional<Doctor> findByIdAndActiveTrue(UUID id);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @EntityGraph(attributePaths={"user","application","specialties"}) @Query("select d from Doctor d where d.id=:id and d.active=true") Optional<Doctor> lockActiveById(@Param("id") UUID id);
}
