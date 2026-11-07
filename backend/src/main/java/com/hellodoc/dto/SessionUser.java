package com.hellodoc.dto;

import com.hellodoc.entity.UserAccount;
import java.util.UUID;

public record SessionUser(UUID id, String firstName, String lastName, String email, String role, String status) {
    public static SessionUser from(UserAccount u) { return new SessionUser(u.getId(),u.getFirstName(),u.getLastName(),u.getEmail(),u.getRole().name(),u.getStatus()); }
}
