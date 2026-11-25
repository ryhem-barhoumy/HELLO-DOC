package com.hellodoc.controller;

import com.hellodoc.config.SecurityConfig;
import com.hellodoc.dto.MeetLinkDto;
import com.hellodoc.service.AppointmentMeetLinkService;
import com.hellodoc.security.AccountDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppointmentMeetLinkController.class)
@Import(SecurityConfig.class)
class AppointmentMeetLinkSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean AppointmentMeetLinkService service;
    @MockBean AccountDetailsService accountDetails;

    @Test void patientMayReadOwnMeetLinkButCannotChangeIt() throws Exception {
        UUID id=UUID.randomUUID();when(service.get("patient@test.invalid",id)).thenReturn(new MeetLinkDto(id,"https://meet.google.com/abc-defg-hij"));
        mvc.perform(get("/api/appointments/"+id+"/meet-link").with(user("patient@test.invalid").roles("USER"))).andExpect(status().isOk());
        mvc.perform(put("/api/appointments/"+id+"/meet-link").with(user("patient@test.invalid").roles("USER")).with(csrf()).contentType("application/json").content("{\"url\":\"https://meet.google.com/abc-defg-hij\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/appointments/"+id+"/meet-link/external-call-ended").with(user("patient@test.invalid").roles("USER")).with(csrf())).andExpect(status().isForbidden());
        verify(service).get("patient@test.invalid",id);verify(service,never()).save(anyString(),any(),any());
    }

    @Test void unauthenticatedAndAdminRequestsCannotFetchParticipantMeetLink() throws Exception {
        UUID id=UUID.randomUUID();mvc.perform(get("/api/appointments/"+id+"/meet-link")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/appointments/"+id+"/meet-link").with(user("admin@test.invalid").roles("ADMIN"))).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
