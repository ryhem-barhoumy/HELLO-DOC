package com.hellodoc.repository;
import com.hellodoc.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;
public interface ReviewRepository extends JpaRepository<Review,UUID> {
 Optional<Review> findByAppointmentId(UUID appointmentId);
 @EntityGraph(attributePaths={"appointment","patient.user","doctor.user"})
 @Query("select r from Review r where r.doctor.id=:doctorId order by r.createdAt desc") Page<Review> findByDoctor(@Param("doctorId") UUID doctorId,Pageable pageable);
 @Query("select r from Review r where r.appointment.id=:appointmentId and lower(r.patient.user.email)=lower(:email)") Optional<Review> findOwned(@Param("appointmentId") UUID appointmentId,@Param("email") String email);
 @Query("select avg(r.rating) from Review r where r.doctor.id=:doctorId") Double averageRating(@Param("doctorId") UUID doctorId);
 long countByDoctorId(UUID doctorId);
}
