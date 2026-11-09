package com.hellodoc.controller;

import com.hellodoc.config.SecurityConfig;
import com.hellodoc.security.AccountDetailsService;
import com.hellodoc.service.SpecialtyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SpecialtyController.class)
@Import(SecurityConfig.class)
class PublicSpecialtySecurityTest {
 @Autowired MockMvc mvc;
 @MockBean SpecialtyService specialties;
 @MockBean AccountDetailsService accounts;
 @Test void specialtyListIsPublicReadOnly() throws Exception {when(specialties.list()).thenReturn(java.util.List.of());mvc.perform(get("/api/specialties")).andExpect(status().isOk());verify(specialties).list();}
}
