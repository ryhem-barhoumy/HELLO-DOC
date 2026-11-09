package com.hellodoc.controller;

import com.hellodoc.dto.*;
import com.hellodoc.service.AppointmentService;
import com.hellodoc.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentService appointments;
    private final ReviewService reviews;
    public AppointmentController(AppointmentService appointments,ReviewService reviews){this.appointments=appointments;this.reviews=reviews;}

    @PostMapping @PreAuthorize("hasAnyRole('USER','PATIENT')") @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public AppointmentDto book(Authentication auth,@Valid @RequestBody AppointmentCreateRequest request){return appointments.book(auth.getName(),request);}
    @GetMapping("/mine") @PreAuthorize("hasAnyRole('USER','PATIENT')")
    public PagedResponse<AppointmentDto> mine(Authentication auth,@RequestParam(defaultValue="false") boolean history,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="30") int size){return appointments.patientAppointments(auth.getName(),history,page,size);}
    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('USER','PATIENT','DOCTOR')")
    public AppointmentDto get(Authentication auth,@PathVariable UUID id){return appointments.get(auth.getName(),id);}
    @PostMapping("/{id}/cancellation") @PreAuthorize("hasAnyRole('USER','PATIENT','DOCTOR')")
    public AppointmentDto cancel(Authentication auth,@PathVariable UUID id,@Valid @RequestBody(required=false) DecisionRequest request){return appointments.cancel(auth.getName(),id,request==null?null:request.reason());}
    @PatchMapping("/{id}/status") @PreAuthorize("hasRole('DOCTOR')")
    public AppointmentDto status(Authentication auth,@PathVariable UUID id,@Valid @RequestBody AppointmentStatusRequest request){return appointments.changeStatus(auth.getName(),id,request);}
    @GetMapping("/{id}/reschedule-options") @PreAuthorize("hasAnyRole('USER','PATIENT','DOCTOR')")
    public java.util.List<com.hellodoc.dto.TimeSlotDto> rescheduleOptions(Authentication auth,@PathVariable UUID id,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date){return appointments.rescheduleOptions(auth.getName(),id,date);}
    @PutMapping("/{id}/reschedule") @PreAuthorize("hasAnyRole('USER','PATIENT','DOCTOR')")
    public AppointmentDto reschedule(Authentication auth,@PathVariable UUID id,@Valid @RequestBody AppointmentRescheduleRequest request){return appointments.reschedule(auth.getName(),id,request);}
    @PostMapping("/{id}/review") @PreAuthorize("hasAnyRole('USER','PATIENT')")
    public ReviewDto review(Authentication auth,@PathVariable UUID id,@Valid @RequestBody ReviewCreateRequest request){return reviews.save(auth.getName(),id,request);}
}
