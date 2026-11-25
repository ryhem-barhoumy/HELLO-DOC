package com.hellodoc.controller;

import com.hellodoc.config.SecurityConfig;
import com.hellodoc.dto.AppointmentDto;
import com.hellodoc.dto.PagedResponse;
import com.hellodoc.entity.AppointmentStatus;
import com.hellodoc.security.AccountDetailsService;
import com.hellodoc.service.AppointmentService;
import com.hellodoc.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppointmentController.class)
@Import(SecurityConfig.class)
class PatientAppointmentApiCompatibilityTest {
    @Autowired MockMvc mvc;
    @MockBean AppointmentService appointments;
    @MockBean ReviewService reviews;
    @MockBean AccountDetailsService accountDetails;

    @Test
    void patientAppointmentHistoryRouteAndPagingJsonRemainAvailable() throws Exception {
        when(appointments.patientAppointments("patient",true,0,30)).thenReturn(PagedResponse.from(Page.empty()));
        mvc.perform(get("/api/appointments/mine").param("history","true").with(user("patient").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
        verify(appointments).patientAppointments("patient",true,0,30);
    }

    @Test
    void patientBookingRouteRetainsCreatedStatusAndAppointmentJson() throws Exception {
        UUID doctorId=UUID.randomUUID(),appointmentId=UUID.randomUUID();
        AppointmentDto response=new AppointmentDto(appointmentId,Instant.parse("2030-04-15T09:00:00Z"),Instant.parse("2030-04-15T09:30:00Z"),AppointmentStatus.PENDING,"Consultation","50","Pat Ient","Dr Test","Clinic",null,null,List.of(),null);
        when(appointments.book(eq("patient"),any())).thenReturn(response);
        mvc.perform(post("/api/appointments").with(user("patient").roles("PATIENT")).with(csrf())
                        .contentType("application/json")
                        .content("{\"doctorId\":\""+doctorId+"\",\"serviceName\":\"Consultation\",\"startAt\":\"2030-04-15T09:00:00Z\",\"reason\":\"Follow-up\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(appointmentId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.serviceName").value("Consultation"));
        verify(appointments).book(eq("patient"),any());
    }

    @Test
    void appointmentHistoryRejectsAnonymousAndDoctorRequests() throws Exception {
        mvc.perform(get("/api/appointments/mine")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/appointments/mine").with(user("doctor").roles("DOCTOR"))).andExpect(status().isForbidden());
        verifyNoInteractions(appointments);
    }
}
