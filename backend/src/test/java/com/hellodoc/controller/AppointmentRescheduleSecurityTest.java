package com.hellodoc.controller;

import com.hellodoc.config.SecurityConfig;
import com.hellodoc.security.AccountDetailsService;
import com.hellodoc.service.AppointmentService;
import com.hellodoc.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppointmentController.class)
@Import(SecurityConfig.class)
class AppointmentRescheduleSecurityTest {
 @Autowired MockMvc mvc;
 @MockBean AppointmentService appointments;
 @MockBean ReviewService reviews;
 @MockBean AccountDetailsService accounts;
 final String base="/api/appointments/"+UUID.randomUUID();
 @Test void doctorCannotGetSlotsOrReprogramEvenWhenSendingDirectApiRequests() throws Exception {
  mvc.perform(get(base+"/reschedule-options").param("date","2030-01-15").with(user("doctor").roles("DOCTOR"))).andExpect(status().isForbidden());
  mvc.perform(put(base+"/reschedule").with(user("doctor").roles("DOCTOR")).with(csrf()).contentType("application/json").content("{\"startAt\":\"2030-01-15T09:00:00Z\"}")).andExpect(status().isForbidden());
  verifyNoInteractions(appointments);
 }
 @Test void bothPatientRoleNamesCanUsePatientReschedulingEndpoints() throws Exception {
  for(String role:new String[]{"USER","PATIENT"}){
   mvc.perform(get(base+"/reschedule-options").param("date","2030-01-15").with(user("patient").roles(role))).andExpect(status().isOk());
   mvc.perform(put(base+"/reschedule").with(user("patient").roles(role)).with(csrf()).contentType("application/json").content("{\"startAt\":\"2030-01-15T09:00:00Z\"}")).andExpect(status().isOk());
  }
 }
 @Test void anonymousRequestsAndMissingCsrfAreRejected() throws Exception {
  mvc.perform(get(base+"/reschedule-options").param("date","2030-01-15")).andExpect(status().isUnauthorized());
  mvc.perform(put(base+"/reschedule").with(user("patient").roles("USER")).contentType("application/json").content("{\"startAt\":\"2030-01-15T09:00:00Z\"}")).andExpect(status().isForbidden());
  verifyNoInteractions(appointments);
 }
}
