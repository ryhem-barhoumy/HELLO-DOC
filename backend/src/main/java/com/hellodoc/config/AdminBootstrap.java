package com.hellodoc.config;

import com.hellodoc.entity.UserAccount;
import com.hellodoc.entity.UserRole;
import com.hellodoc.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminBootstrap {
 @Bean ApplicationRunner seedAdmin(UserRepository users, PasswordEncoder encoder,
   @Value("${hellodoc.admin.email}") String email, @Value("${hellodoc.admin.initial-password}") String initialPassword) {
  return args -> {
   var existing = users.findByEmailIgnoreCase(email);
   if (existing.isPresent()) {
    if (existing.get().getRole() != UserRole.ADMIN) throw new IllegalStateException("The configured administrator email is already assigned to a non-admin account.");
    return;
   }
   if (initialPassword == null || initialPassword.isBlank()) throw new IllegalStateException("Set HELLODOC_ADMIN_INITIAL_PASSWORD to seed the initial administrator.");
   if (initialPassword.length() < 12) throw new IllegalStateException("HELLODOC_ADMIN_INITIAL_PASSWORD must contain at least 12 characters.");
   users.save(new UserAccount("Hello", "Doc Admin", email.toLowerCase(), encoder.encode(initialPassword), UserRole.ADMIN, "ACTIVE"));
  };
 }
}
