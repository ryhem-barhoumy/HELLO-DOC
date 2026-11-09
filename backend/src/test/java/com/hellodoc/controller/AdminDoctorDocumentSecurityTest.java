package com.hellodoc.controller;

import com.hellodoc.config.SecurityConfig;
import com.hellodoc.security.AccountDetailsService;
import com.hellodoc.service.DoctorDocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminDoctorDocumentController.class)
@Import(SecurityConfig.class)
class AdminDoctorDocumentSecurityTest {
 @Autowired MockMvc mvc;
 @MockBean DoctorDocumentService documents;
 @MockBean AccountDetailsService accounts;
 private final UUID id=UUID.randomUUID();
 @Test void administratorCanDownloadDocument() throws Exception {
  when(documents.download(id)).thenReturn(new DoctorDocumentService.DownloadedDocument(new byte[]{1,2,3},"proof.pdf","application/pdf",3));
  mvc.perform(get("/api/admin/doctors/{id}/document",id).with(user("admin").roles("ADMIN")))
   .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF)).andExpect(content().bytes(new byte[]{1,2,3}));
 }
 @Test void patientAndDoctorCannotDownloadDocument() throws Exception {
  mvc.perform(get("/api/admin/doctors/{id}/document",id).with(user("patient").roles("USER"))).andExpect(status().isForbidden());
  mvc.perform(get("/api/admin/doctors/{id}/document",id).with(user("doctor").roles("DOCTOR"))).andExpect(status().isForbidden());
 }
}
