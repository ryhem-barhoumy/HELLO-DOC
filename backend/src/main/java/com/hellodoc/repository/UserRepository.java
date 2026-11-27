package com.hellodoc.repository;

import com.hellodoc.entity.UserAccount;
import com.hellodoc.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);
    long countByRole(UserRole role);
    long countByRoleAndStatus(UserRole role,String status);
    boolean existsByEmailIgnoreCase(String email);
    List<UserAccount> findAllByRoleIn(Collection<UserRole> roles);

    @Modifying
    @Transactional
    @Query("update UserAccount u set u.passwordHash = :replacement where u.id = :id and u.passwordHash = :previous and u.status = 'ACTIVE'")
    int replaceActivePassword(@Param("id") UUID id, @Param("previous") String previous,
                              @Param("replacement") String replacement);
}
