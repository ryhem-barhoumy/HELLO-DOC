package com.hellodoc.controller;
import com.hellodoc.dto.*;
import com.hellodoc.entity.AppointmentStatus;
import com.hellodoc.service.AdminOperationsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;
@RestController @RequestMapping("/api/admin") @PreAuthorize("hasRole('ADMIN')")
public class AdminOperationsController {
 private final AdminOperationsService operations;
 public AdminOperationsController(AdminOperationsService operations){this.operations=operations;}
 @GetMapping("/overview") public Map<String,Object> overview(){return operations.overview();}
 @GetMapping("/inbox") public Map<String,Object> inbox(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return operations.inbox(page,size);}
 @GetMapping("/appointments") public PagedResponse<AdminAppointmentDto> appointments(@RequestParam(required=false) AppointmentStatus status,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant from,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) Instant to,@RequestParam(required=false) String q,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return operations.appointments(status,from,to,q,page,size);}
 @GetMapping("/appointments/{id}") public AdminAppointmentDto appointment(@PathVariable UUID id){return operations.appointment(id);}
 @GetMapping("/activity-logs") public PagedResponse<AdministrativeActivityDto> activities(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return operations.activities(page,size);}
}
