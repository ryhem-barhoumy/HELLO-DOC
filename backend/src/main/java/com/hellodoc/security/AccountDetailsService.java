package com.hellodoc.security;

import com.hellodoc.entity.UserAccount;
import com.hellodoc.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class AccountDetailsService implements UserDetailsService {
    private final UserRepository users;
    public AccountDetailsService(UserRepository users){this.users=users;}
    @Override public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount u=users.findByEmailIgnoreCase(username.trim()).orElseThrow(()->new UsernameNotFoundException("Invalid credentials"));
        return User.withUsername(u.getEmail()).password(u.getPasswordHash()).accountLocked("PENDING".equals(u.getStatus())).disabled("INACTIVE".equals(u.getStatus())||"REJECTED".equals(u.getStatus())).authorities(new SimpleGrantedAuthority("ROLE_"+u.getRole().name())).build();
    }
}
