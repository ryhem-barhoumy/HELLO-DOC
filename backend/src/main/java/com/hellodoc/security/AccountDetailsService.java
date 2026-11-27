package com.hellodoc.security;

import com.hellodoc.entity.UserAccount;
import com.hellodoc.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountDetailsService implements UserDetailsService {
    private final UserRepository users;
    public AccountDetailsService(UserRepository users){this.users=users;}
    @Override public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount u=users.findByEmailIgnoreCase(username.trim()).orElseThrow(()->new UsernameNotFoundException("Invalid credentials"));
        return new AccountPrincipal(u);
    }

    public static final class AccountPrincipal extends User {
        private final String credentialVersion;

        private AccountPrincipal(UserAccount user) {
            super(user.getEmail(), user.getPasswordHash(),
                    !"INACTIVE".equals(user.getStatus()) && !"REJECTED".equals(user.getStatus()),
                    true, true, !"PENDING".equals(user.getStatus()),
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
            credentialVersion = AuthenticatedSession.credentialVersion(user);
        }

        public String credentialVersion() { return credentialVersion; }
    }
}
