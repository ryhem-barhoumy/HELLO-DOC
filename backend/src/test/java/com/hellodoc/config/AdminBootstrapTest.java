package com.hellodoc.config;

import com.hellodoc.entity.UserAccount;
import com.hellodoc.entity.UserRole;
import com.hellodoc.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminBootstrapTest {
 @Test void createsOneBcryptAdminAndLeavesItUntouchedOnNextStart() throws Exception {
  UserRepository users=mock(UserRepository.class); BCryptPasswordEncoder encoder=new BCryptPasswordEncoder();
  AtomicReference<UserAccount> persisted=new AtomicReference<>();
  when(users.findByEmailIgnoreCase("admin@hellodoc.com")).thenAnswer(invocation->Optional.ofNullable(persisted.get()));
  when(users.save(any(UserAccount.class))).thenAnswer(invocation->{UserAccount account=invocation.getArgument(0);persisted.set(account);return account;});
  var seed=new AdminBootstrap().seedAdmin(users,encoder,"admin@hellodoc.com","Initial-test-password-123");
  seed.run(mock(ApplicationArguments.class)); seed.run(mock(ApplicationArguments.class));
  assertEquals(UserRole.ADMIN,persisted.get().getRole());
  assertTrue(encoder.matches("Initial-test-password-123",persisted.get().getPasswordHash()));
  verify(users,times(1)).save(any(UserAccount.class));
 }
}
