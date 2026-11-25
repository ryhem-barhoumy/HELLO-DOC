package com.hellodoc.controller;

import com.hellodoc.dto.MeetLinkDto;
import com.hellodoc.dto.MeetLinkRequest;
import com.hellodoc.service.AppointmentMeetLinkService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/appointments/{id}/meet-link")
public class AppointmentMeetLinkController {
    private final AppointmentMeetLinkService service;
    public AppointmentMeetLinkController(AppointmentMeetLinkService service){this.service=service;}

    @GetMapping @PreAuthorize("hasAnyRole('DOCTOR','USER','PATIENT')")
    public MeetLinkDto get(Authentication auth,@PathVariable UUID id){return service.get(auth.getName(),id);}

    @PutMapping @PreAuthorize("hasRole('DOCTOR')")
    public MeetLinkDto save(Authentication auth,@PathVariable UUID id,@Valid @RequestBody MeetLinkRequest request){return service.save(auth.getName(),id,request);}

    @DeleteMapping @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('DOCTOR')")
    public void remove(Authentication auth,@PathVariable UUID id){service.remove(auth.getName(),id);}

    @PostMapping("/external-call-ended") @PreAuthorize("hasRole('DOCTOR')")
    public MeetLinkDto markExternalCallEnded(Authentication auth,@PathVariable UUID id){return service.markExternalCallEnded(auth.getName(),id);}
}
