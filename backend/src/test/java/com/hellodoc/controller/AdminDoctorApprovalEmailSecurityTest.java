package com.hellodoc.controller;

import com.hellodoc.config.SecurityConfig;
import com.hellodoc.dto.ApprovalEmailStatus;
import com.hellodoc.security.AccountDetailsService;
import com.hellodoc.service.DoctorApprovalEmailDeliveryService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminDoctorApprovalEmailController.class)
@Import(SecurityConfig.class)
class AdminDoctorApprovalEmailSecurityTest {
 @Autowired MockMvc mvc;
 @MockBean DoctorApprovalEmailDeliveryService emails;
 @MockBean AccountDetailsService accounts;
 final UUID id=UUID.randomUUID();
 String path(){return "/api/admin/doctors/"+id+"/approval-email";}
 @Test void anonymousAndNonAdminCannotInspectOrSendMail() throws Exception {
  mvc.perform(get(path())).andExpect(status().isUnauthorized());
  mvc.perform(get(path()).with(user("patient").roles("USER"))).andExpect(status().isForbidden());
  mvc.perform(post(path()).with(user("doctor").roles("DOCTOR")).with(csrf())).andExpect(status().isForbidden());
  verifyNoInteractions(emails);
 }
 @Test void administratorCanReadStatusAndQueueRetry() throws Exception {
  when(emails.status(id)).thenReturn(new ApprovalEmailStatus("FAILED","Échec d’authentification SMTP",null,null));
  when(emails.retry(id)).thenReturn(new ApprovalEmailStatus("QUEUED","Envoi en cours",null,null));
  mvc.perform(get(path()).with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FAILED"));
  mvc.perform(post(path()).with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("QUEUED"));
 }
 @Test void retryRequiresCsrfEvenForAdministrator() throws Exception {
  mvc.perform(post(path()).with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());verifyNoInteractions(emails);
 }
}
