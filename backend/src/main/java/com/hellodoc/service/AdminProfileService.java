package com.hellodoc.service;

import com.hellodoc.dto.PatientProfileDto;
import com.hellodoc.dto.PatientProfileUpdateRequest;
import com.hellodoc.entity.UserAccount;
import com.hellodoc.entity.UserRole;
import com.hellodoc.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminProfileService {
    private final UserRepository users;
    private final PasswordEncoder passwords;

    public AdminProfileService(UserRepository users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    @Transactional(readOnly = true)
    public PatientProfileDto get(String email) { return dto(own(email)); }

    @Transactional
    public PatientProfileDto update(String email, PatientProfileUpdateRequest request) {
        UserAccount user = own(email);
        if (!passwords.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe actuel est incorrect.");
        }
        user.updateContactProfile(request.firstName().trim(), request.lastName().trim(),
                clean(request.phone()), clean(request.address()));
        return dto(user);
    }

    private UserAccount own(String email) {
        return users.findByEmailIgnoreCase(email)
                .filter(u -> u.getRole() == UserRole.ADMIN && "ACTIVE".equals(u.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Un compte administrateur actif est requis."));
    }

    private PatientProfileDto dto(UserAccount user) {
        return new PatientProfileDto(user.getId(), user.getFirstName(), user.getLastName(),
                user.getFirstName() + " " + user.getLastName(), user.getEmail(), user.getPhone(), user.getAddress());
    }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
