package com.hellodoc.repository;

import com.hellodoc.entity.UserAccount;
import com.hellodoc.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);
    long countByRole(UserRole role);
    long countByRoleAndStatus(UserRole role,String status);
    boolean existsByEmailIgnoreCase(String email);
}
