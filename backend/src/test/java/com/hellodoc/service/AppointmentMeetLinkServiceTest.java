package com.hellodoc.service;

import com.hellodoc.dto.MeetLinkRequest;
import com.hellodoc.entity.*;
import com.hellodoc.repository.AppointmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppointmentMeetLinkServiceTest {
    @Test void doctorCanSaveAndParticipantsCanReadOnlyTheirConfirmedVideoMeeting(){
        var appointments=mock(AppointmentRepository.class);var doctors=mock(DoctorProfileService.class);String email="meet-doctor@test.invalid";
        var account=new UserAccount("Doc","Tor",email,"hash",UserRole.DOCTOR,"ACTIVE");var doctor=mock(Doctor.class);when(doctor.getUser()).thenReturn(account);when(doctors.own(email)).thenReturn(doctor);
        var patient=new Patient(new UserAccount("Pat","Ient","meet-patient@test.invalid","hash",UserRole.USER,"ACTIVE"));var appointment=new Appointment(doctor,patient,"Consultation","50",Instant.now(),Instant.now().plusSeconds(1800),null,ConsultationMode.VIDEO);appointment.changeStatus(AppointmentStatus.CONFIRMED,null);UUID id=UUID.randomUUID();org.springframework.test.util.ReflectionTestUtils.setField(appointment,"id",id);
        when(appointments.findDoctorOwned(id,email)).thenReturn(Optional.of(appointment));when(appointments.findOwned(id,patient.getUser().getEmail())).thenReturn(Optional.of(appointment));var service=new AppointmentMeetLinkService(appointments,doctors);
        assertEquals("https://meet.google.com/abc-defg-hij",service.save(email,id,new MeetLinkRequest(" https://meet.google.com/abc-defg-hij ")).url());assertEquals("https://meet.google.com/abc-defg-hij",service.get(patient.getUser().getEmail(),id).url());var ended=service.markExternalCallEnded(email,id);assertNotNull(ended.externalCallEndedAt());assertEquals(email,ended.externalCallEndedBy());assertEquals(AppointmentStatus.CONFIRMED,appointment.getStatus());
    }
    @Test void rejectsOtherDoctorsAndUnsafeOrUnrelatedUrls(){var appointments=mock(AppointmentRepository.class);var doctors=mock(DoctorProfileService.class);String email="other@test.invalid";when(doctors.own(email)).thenReturn(mock(Doctor.class));UUID id=UUID.randomUUID();when(appointments.findDoctorOwned(id,email)).thenReturn(Optional.empty());var service=new AppointmentMeetLinkService(appointments,doctors);assertEquals(404,assertThrows(ResponseStatusException.class,()->service.save(email,id,new MeetLinkRequest("https://meet.google.com/abc-defg-hij"))).getStatusCode().value());}
    @Test void participantLookupDoesNotRevealLinksToAnotherAccount(){var appointments=mock(AppointmentRepository.class);UUID id=UUID.randomUUID();when(appointments.findOwned(id,"stranger@test.invalid")).thenReturn(Optional.empty());var service=new AppointmentMeetLinkService(appointments,mock(DoctorProfileService.class));assertEquals(404,assertThrows(ResponseStatusException.class,()->service.get("stranger@test.invalid",id)).getStatusCode().value());verify(appointments).findOwned(id,"stranger@test.invalid");}
    @Test void patientCannotRetrieveMeetLinkOutsideTheVideoWindow(){var appointments=mock(AppointmentRepository.class);var doctor=mock(Doctor.class);var patient=new Patient(new UserAccount("Pat","Ient","patient@test.invalid","hash",UserRole.USER,"ACTIVE"));var appointment=new Appointment(doctor,patient,"Consultation","50",Instant.now().plusSeconds(3600),Instant.now().plusSeconds(5400),null,ConsultationMode.VIDEO);appointment.changeStatus(AppointmentStatus.CONFIRMED,null);appointment.setMeetLink("https://meet.google.com/abc-defg-hij");UUID id=UUID.randomUUID();when(appointments.findOwned(id,patient.getUser().getEmail())).thenReturn(Optional.of(appointment));var service=new AppointmentMeetLinkService(appointments,mock(DoctorProfileService.class));assertEquals(404,assertThrows(ResponseStatusException.class,()->service.get(patient.getUser().getEmail(),id)).getStatusCode().value());}
    @Test void rejectsNonMeetHostsAndLinksContainingQueryOrFragment(){var appointments=mock(AppointmentRepository.class);var doctors=mock(DoctorProfileService.class);String email="doctor@test.invalid";var doctor=mock(Doctor.class);when(doctor.getUser()).thenReturn(new UserAccount("Doc","Tor",email,"hash",UserRole.DOCTOR,"ACTIVE"));when(doctors.own(email)).thenReturn(doctor);var appointment=new Appointment(doctor,new Patient(new UserAccount("Pat","Ient","patient@test.invalid","hash",UserRole.USER,"ACTIVE")),"Consultation","50",Instant.now(),Instant.now().plusSeconds(1800),null,ConsultationMode.VIDEO);appointment.changeStatus(AppointmentStatus.CONFIRMED,null);UUID id=UUID.randomUUID();when(appointments.findDoctorOwned(id,email)).thenReturn(Optional.of(appointment));var service=new AppointmentMeetLinkService(appointments,doctors);for(String url:new String[]{"https://evil.example/abc-defg-hij","https://meet.google.com/abc-defg-hij?authuser=0","https://meet.google.com/abc-defg-hij#secret","http://meet.google.com/abc-defg-hij"})assertEquals(400,assertThrows(ResponseStatusException.class,()->service.save(email,id,new MeetLinkRequest(url))).getStatusCode().value());assertNull(appointment.getMeetLink());}
}
