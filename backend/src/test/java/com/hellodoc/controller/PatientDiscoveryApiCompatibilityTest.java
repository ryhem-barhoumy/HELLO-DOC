package com.hellodoc.controller;

import com.hellodoc.config.SecurityConfig;
import com.hellodoc.dto.DoctorPublicDto;
import com.hellodoc.dto.DoctorSearchDto;
import com.hellodoc.dto.TimeSlotDto;
import com.hellodoc.security.AccountDetailsService;
import com.hellodoc.service.AppointmentService;
import com.hellodoc.service.DoctorProfileService;
import com.hellodoc.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DoctorController.class)
@Import(SecurityConfig.class)
class PatientDiscoveryApiCompatibilityTest {
    @Autowired MockMvc mvc;
    @MockBean DoctorProfileService doctors;
    @MockBean AppointmentService appointments;
    @MockBean ReviewService reviews;
    @MockBean AccountDetailsService accountDetails;

    @Test
    void doctorSearchAndAvailabilityRemainPublicAndKeepTheirResponseShapes() throws Exception {
        UUID doctorId=UUID.randomUUID();
        when(doctors.search("cardio",null,null,null,0,20,false)).thenReturn(Page.empty());
        when(appointments.freeSlots(doctorId,LocalDate.parse("2030-04-15"))).thenReturn(List.of(new TimeSlotDto("slot-1",Instant.parse("2030-04-15T09:00:00Z"),Instant.parse("2030-04-15T09:30:00Z"),"AVAILABLE")));

        mvc.perform(get("/api/doctors").param("q","cardio"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
        mvc.perform(get("/api/doctors/{id}/slots",doctorId).param("date","2030-04-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("slot-1"))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$[0].startsAt").value("2030-04-15T09:00:00Z"));
        verify(doctors).search("cardio",null,null,null,0,20,false);
        verify(appointments).freeSlots(doctorId,LocalDate.parse("2030-04-15"));
    }

    @Test
    void publicProfileRedactsAccountPhoneAndPatientSessionKeepsExistingContract() throws Exception {
        UUID id = UUID.randomUUID();
        var profile = new DoctorPublicDto(id, "Nadia", "Haddad", "Nadia Haddad", "Cabinet Haddad",
                "Cardiologie", List.of("Cardiologie"), "Tunis", "Cabinet à Tunis", "12 rue du Cabinet",
                "20123456", "Consultations cardiologiques", 30, List.of(1), List.of(), null, null, true);
        when(doctors.publicProfile(id)).thenReturn(profile);

        mvc.perform(get("/api/doctors/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Nadia Haddad"))
                .andExpect(jsonPath("$.videoConsultationEnabled").value(true))
                .andExpect(jsonPath("$.location").value("Cabinet à Tunis"))
                .andExpect(jsonPath("$.address").doesNotExist())
                .andExpect(jsonPath("$.phone").doesNotExist());
        mvc.perform(get("/api/doctors/{id}", id).with(user("patient@example.test").roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("12 rue du Cabinet"))
                .andExpect(jsonPath("$.phone").value("20123456"));
    }

    @Test
    void anonymousSearchRedactsPrivateAddressAndLegacyLocationOnly() throws Exception {
        var inherited = new DoctorSearchDto(UUID.randomUUID(), "Nadia", "Haddad", "Nadia Haddad", "Cabinet Haddad",
                "Cardiologie", "Tunis", "20 rue privée", "20 rue privée", "ACTIVE", null, null, false);
        var published = new DoctorSearchDto(UUID.randomUUID(), "Amel", "Kallel", "Amel Kallel", "Cabinet Kallel",
                "Cardiologie", "Tunis", "Lieu du cabinet", "21 rue privée", "ACTIVE", null, null, false);
        when(doctors.search(null,null,"Tunis",null,0,20,false)).thenReturn(new PageImpl<>(List.of(inherited, published)));

        mvc.perform(get("/api/doctors").param("city", "Tunis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].city").value("Tunis"))
                .andExpect(jsonPath("$.content[0].location").doesNotExist())
                .andExpect(jsonPath("$.content[0].address").doesNotExist())
                .andExpect(jsonPath("$.content[1].location").value("Lieu du cabinet"))
                .andExpect(jsonPath("$.content[1].address").doesNotExist());
    }

    @Test
    void anonymousDirectoryAccessDoesNotAuthorizeBookingOrPrivateResources() throws Exception {
        mvc.perform(post("/api/appointments").with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/patients/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/doctors/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/doctors")).andExpect(status().isUnauthorized());
    }
}
