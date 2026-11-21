package com.hellodoc.controller;

import com.hellodoc.dto.ApprovalEmailStatus;
import com.hellodoc.service.DoctorApprovalEmailDeliveryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/doctors/{id}/approval-email")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDoctorApprovalEmailController {
    private final DoctorApprovalEmailDeliveryService emails;
    public AdminDoctorApprovalEmailController(DoctorApprovalEmailDeliveryService emails){this.emails=emails;}
    @GetMapping public ApprovalEmailStatus status(@PathVariable UUID id){return emails.status(id);}
    @PostMapping public ApprovalEmailStatus retry(@PathVariable UUID id){return emails.retry(id);}
}
