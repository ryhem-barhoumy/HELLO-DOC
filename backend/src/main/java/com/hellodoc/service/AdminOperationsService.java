package com.hellodoc.service;
import com.hellodoc.dto.*;
import com.hellodoc.entity.*;
import com.hellodoc.repository.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
@Service @Transactional(readOnly=true)
public class AdminOperationsService {
 private final AppointmentRepository appointments;private final AdministrativeActivityRepository activities;private final UserRepository users;private final DoctorApplicationRepository applications;private final DoctorRepository doctors;
 public AdminOperationsService(AppointmentRepository appointments,AdministrativeActivityRepository activities,UserRepository users,DoctorApplicationRepository applications,DoctorRepository doctors){this.appointments=appointments;this.activities=activities;this.users=users;this.applications=applications;this.doctors=doctors;}
 public Map<String,Object> inbox(int page,int size){return Map.of("pendingApprovals",applications.countByStatus("PENDING"),"failedEmails",PagedResponse.from(doctors.findByApplicationApprovalEmailStatusOrderByIdDesc("FAILED",PageRequest.of(Math.max(0,page),Math.max(1,Math.min(50,size)))).map(AdminDoctorDto::from)));}
 public PagedResponse<AdminAppointmentDto> appointments(AppointmentStatus status,Instant from,Instant to,String q,int page,int size){if(from!=null&&to!=null&&!to.isAfter(from))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La fin de période doit suivre son début.");String query=q==null||q.isBlank()?null:"%"+q.trim().toLowerCase(Locale.ROOT)+"%";return PagedResponse.from(appointments.findOperational(status,from,to,query,PageRequest.of(Math.max(0,page),Math.max(1,Math.min(100,size)))) .map(AdminAppointmentDto::from));}
 public AdminAppointmentDto appointment(UUID id){return appointments.findById(id).map(AdminAppointmentDto::from).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Rendez-vous introuvable."));}
 public PagedResponse<AdministrativeActivityDto> activities(int page,int size){return PagedResponse.from(activities.findAll(PageRequest.of(Math.max(0,page),Math.max(1,Math.min(100,size)),Sort.by("occurredAt").descending())).map(AdministrativeActivityDto::from));}
 public Map<String,Object> overview(){
  Map<String,Object> counts=new LinkedHashMap<>();counts.put("total",appointments.count());counts.put("pending",appointments.countByStatus(AppointmentStatus.PENDING));counts.put("confirmed",appointments.countByStatus(AppointmentStatus.CONFIRMED));counts.put("completed",appointments.countByStatus(AppointmentStatus.COMPLETED));counts.put("cancelled",appointments.countByStatus(AppointmentStatus.CANCELLED));
  ZoneId zone=ZoneId.of("Africa/Tunis");YearMonth current=YearMonth.now(zone);List<Map<String,Object>> months=new ArrayList<>();
  for(int i=5;i>=0;i--){YearMonth m=current.minusMonths(i);months.add(Map.of("month",m.toString(),"label",m.getMonth().getDisplayName(java.time.format.TextStyle.SHORT,Locale.FRENCH)+" "+m.getYear(),"total",appointments.countByStartAtGreaterThanEqualAndStartAtLessThan(m.atDay(1).atStartOfDay(zone).toInstant(),m.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant())));}
  Map<String,Object> result=new LinkedHashMap<>();result.put("asOf",Instant.now());result.put("patientsCount",users.countByRoleAndStatus(UserRole.USER,"ACTIVE")+users.countByRoleAndStatus(UserRole.PATIENT,"ACTIVE"));result.put("doctorsCount",users.countByRole(UserRole.DOCTOR));result.put("pendingDoctorApprovals",applications.countByStatus("PENDING"));result.put("appointments",counts);result.put("appointmentsByMonth",months);result.put("recentActivities",activities.findAll(PageRequest.of(0,6,Sort.by("occurredAt").descending())).map(AdministrativeActivityDto::from).getContent());return result;
 }
}
