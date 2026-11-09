package com.hellodoc.controller;

import com.hellodoc.dto.*;
import com.hellodoc.service.NotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequestMapping("/api/notifications") @PreAuthorize("hasAnyRole('USER','PATIENT','DOCTOR')")
public class NotificationController {
 private final NotificationService notifications;public NotificationController(NotificationService notifications){this.notifications=notifications;}
 @GetMapping public PagedResponse<NotificationDto> list(Authentication auth,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size){return notifications.list(auth.getName(),page,size);}
 @PatchMapping("/{id}/read") public NotificationDto markRead(Authentication auth,@PathVariable UUID id){return notifications.markRead(auth.getName(),id);}
}
