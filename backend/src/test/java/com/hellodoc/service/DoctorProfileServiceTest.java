package com.hellodoc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellodoc.dto.*;
import com.hellodoc.entity.*;
import com.hellodoc.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DoctorProfileServiceTest {
 private final DoctorRepository doctors=mock(DoctorRepository.class);
 private final AvailabilityRepository availabilities=mock(AvailabilityRepository.class);
 private final AbsenceRepository absences=mock(AbsenceRepository.class);
 private final AppointmentRepository appointments=mock(AppointmentRepository.class);
 private final SpecialtyService specialties=mock(SpecialtyService.class);
 private final DoctorProfileService service=new DoctorProfileService(doctors,availabilities,absences,appointments,specialties,NoOpPasswordEncoder.getInstance(),new ObjectMapper());

 @Test void createsDoctorEvenWhenApplicationJsonIsMalformed(){
  UserAccount user=new UserAccount("A","Doctor","a@example.test","p","a","hash",UserRole.USER,"PENDING");
  DoctorApplication application=new DoctorApplication(user,"Dr A","Odontologie","LIC-1","Tunis","Clinic",30,"bad","","x".length(),"{bad","{bad","Bio");
  when(doctors.findByUserEmailIgnoreCase("a@example.test")).thenReturn(Optional.empty());
  when(doctors.save(any(Doctor.class))).thenAnswer(i->i.getArgument(0));
  when(specialties.findOrCreate("Odontologie")).thenReturn(new Specialty("Odontologie"));
  Doctor created=assertDoesNotThrow(()->service.createFromApplication(application));
  assertEquals("[]",created.getServicesJson());
  verify(availabilities,never()).save(any());
 }

 @Test void slotsAreCalculatedFromRecurringAvailabilityAndOmitAbsenceDays(){
  UserAccount user=new UserAccount("A","Doctor","a@example.test","p","a","hash",UserRole.DOCTOR,"ACTIVE");
  Doctor doctor=new Doctor(user,null,"Dr A","Tunis","Clinic","Clinic","Bio",30,15,"[]");
  UUID id=UUID.randomUUID();LocalDate date=LocalDate.now(ZoneId.of("Africa/Tunis")).with(java.time.temporal.TemporalAdjusters.next(DayOfWeek.MONDAY));
  when(doctors.findByIdAndActiveTrue(id)).thenReturn(Optional.of(doctor));
  when(absences.findByDoctorIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(any(),eq(date),eq(date))).thenReturn(List.of());
  when(availabilities.findByDoctorIdAndIsoWeekdayOrderByStartTimeAsc(any(),eq(1))).thenReturn(List.of(new Availability(doctor,1,LocalTime.of(9,0),LocalTime.of(10,30),30,15)));
  assertEquals(2,service.slots(id,date).size());
  when(absences.findByDoctorIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(any(),eq(date),eq(date))).thenReturn(List.of(new Absence(doctor,date,date,"Congé",null)));
  assertTrue(service.slots(id,date).isEmpty());
 }

 @Test void slotsSupportTwoWindowsAndRemoveOnlyThePartialAbsenceOverlap(){
  UserAccount user=new UserAccount("A","Doctor","a@example.test","p","a","hash",UserRole.DOCTOR,"ACTIVE");Doctor doctor=new Doctor(user,null,"Dr A","Tunis","Clinic","Clinic","Bio",30,0,"[]");UUID id=UUID.randomUUID();LocalDate date=LocalDate.now(ZoneId.of("Africa/Tunis")).with(java.time.temporal.TemporalAdjusters.next(DayOfWeek.MONDAY));
  when(doctors.findByIdAndActiveTrue(id)).thenReturn(Optional.of(doctor));when(absences.findByDoctorIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(any(),eq(date),eq(date))).thenReturn(List.of(new Absence(doctor,date,date,LocalTime.of(9,0),LocalTime.of(9,30),"Rendez-vous privé",null)));
  when(availabilities.findByDoctorIdAndIsoWeekdayOrderByStartTimeAsc(any(),eq(1))).thenReturn(List.of(new Availability(doctor,1,LocalTime.of(9,0),LocalTime.of(10,0),30,0),new Availability(doctor,1,LocalTime.of(14,0),LocalTime.of(15,0),30,0)));
  List<TimeSlotDto> slots=service.slots(id,date);assertEquals(3,slots.size());assertEquals("Première plage",slots.get(0).periodLabel());assertEquals("Deuxième plage",slots.get(1).periodLabel());
 }

 @Test void weeklyConfigurationRejectsOverlappingWindowsBeforeDeletingSavedSchedule(){
  UserAccount user=new UserAccount("A","Doctor","a@example.test","p","a","hash",UserRole.DOCTOR,"ACTIVE");Doctor doctor=new Doctor(user,null,"Dr A","Tunis","Clinic","Clinic","Bio",30,0,"[]");when(doctors.findByUserEmailIgnoreCase("a@example.test")).thenReturn(Optional.of(doctor));
  WeeklyAvailabilityRequest request=new WeeklyAvailabilityRequest(30,0,List.of(new WeeklyAvailabilityRequest.Window(1,"09:00","12:00"),new WeeklyAvailabilityRequest.Window(1,"11:55","14:00")));
  assertEquals(400,assertThrows(ResponseStatusException.class,()->service.replaceWeeklyAvailability("a@example.test",request)).getStatusCode().value());verify(availabilities,never()).deleteByDoctorId(any());
 }

 @Test void availabilityLookupIsScopedToAuthenticatedDoctorsOwnProfile(){
  UserAccount user=new UserAccount("A","Doctor","a@example.test","p","a","hash",UserRole.DOCTOR,"ACTIVE");
  Doctor doctor=new Doctor(user,null,"Dr A","Tunis","Clinic","Clinic","Bio",30,0,"[]");
  when(doctors.findByUserEmailIgnoreCase("a@example.test")).thenReturn(Optional.of(doctor));
  UUID foreignId=UUID.randomUUID();when(availabilities.findByIdAndDoctorId(foreignId,null)).thenReturn(Optional.empty());
  assertThrows(ResponseStatusException.class,()->service.updateAvailability("a@example.test",foreignId,new AvailabilityRequest(1,"09:00","12:00",30,0)));
 }

 @Test void searchUsesFiltersAndPaginationInRepository(){
  when(doctors.search(eq("Amel"),eq("Tunis"),eq("odontologie"),any(Pageable.class))).thenReturn(Page.empty(PageRequest.of(0,10)));
  assertTrue(service.search("Amel","Odontologie","Tunis",null,0,10).isEmpty());
  verify(doctors).search(eq("Amel"),eq("Tunis"),eq("odontologie"),any(Pageable.class));
 }
}
