package com.hellodoc.controller;

import com.hellodoc.config.SecurityConfig;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        when(doctors.search("cardio",null,null,null,0,20)).thenReturn(Page.empty());
        when(appointments.freeSlots(doctorId,LocalDate.parse("2030-04-15"))).thenReturn(List.of(new TimeSlotDto("slot-1",Instant.parse("2030-04-15T09:00:00Z"),Instant.parse("2030-04-15T09:30:00Z"),"AVAILABLE")));

        mvc.perform(get("/api/doctors").param("q","cardio"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
        mvc.perform(get("/api/doctors/{id}/slots",doctorId).param("date","2030-04-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("slot-1"))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$[0].startsAt").value("2030-04-15T09:00:00Z"));
        verify(doctors).search("cardio",null,null,null,0,20);
        verify(appointments).freeSlots(doctorId,LocalDate.parse("2030-04-15"));
    }
}
