package com.hellodoc.repository;
import com.hellodoc.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;
public interface NotificationRepository extends JpaRepository<Notification,UUID> {
 @Query("select n from Notification n where lower(n.recipient.email)=lower(:email) order by n.createdAt desc") Page<Notification> findMine(@Param("email") String email,Pageable pageable);
 @Query("select count(n) from Notification n where n.recipient.id=:userId and n.readAt is null") long countUnread(@Param("userId") UUID userId);
 @Query("select n from Notification n where n.id=:id and lower(n.recipient.email)=lower(:email)") Optional<Notification> findOwned(@Param("id") UUID id,@Param("email") String email);
}
