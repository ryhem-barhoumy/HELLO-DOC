package com.hellodoc.controller;

import com.hellodoc.config.SecurityConfig;
import com.hellodoc.dto.PatientDashboardDto;
import com.hellodoc.dto.PatientProfileDto;
import com.hellodoc.security.AccountDetailsService;
import com.hellodoc.service.AppointmentService;
import com.hellodoc.service.PatientProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PatientController.class)
@Import(SecurityConfig.class)
class PatientApiCompatibilityTest {
    @Autowired MockMvc mvc;
    @MockBean PatientProfileService patients;
    @MockBean AppointmentService appointments;
    @MockBean AccountDetailsService accountDetails;

    @Test
    void existingProfileAndDashboardRoutesKeepTheirPatientJsonContracts() throws Exception {
        UUID patientId = UUID.randomUUID();
        when(patients.get("patient@example.test")).thenReturn(new PatientProfileDto(patientId,"Pat","Ient","Pat Ient","patient@example.test","12345678","Address",null));
        when(appointments.patientDashboard("patient@example.test")).thenReturn(new PatientDashboardDto(2,3,1,null));

        mvc.perform(get("/api/patients/me").with(user("patient@example.test").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(patientId.toString()))
                .andExpect(jsonPath("$.fullName").value("Pat Ient"))
                .andExpect(jsonPath("$.email").value("patient@example.test"));
        mvc.perform(get("/api/patients/me/dashboard").with(user("patient@example.test").roles("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upcomingCount").value(2))
                .andExpect(jsonPath("$.pastCount").value(3))
                .andExpect(jsonPath("$.unreadNotificationCount").value(1));
        verify(patients).get("patient@example.test");
        verify(appointments).patientDashboard("patient@example.test");
    }

    @Test
    void profileRemainsPrivateToAuthenticatedPatientAccounts() throws Exception {
        mvc.perform(get("/api/patients/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/patients/me").with(user("doctor").roles("DOCTOR"))).andExpect(status().isForbidden());
        verifyNoInteractions(patients, appointments);
    }
}
