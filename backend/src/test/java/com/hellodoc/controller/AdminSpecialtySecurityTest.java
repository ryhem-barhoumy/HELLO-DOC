package com.hellodoc.controller;

import com.hellodoc.config.SecurityConfig;
import com.hellodoc.dto.SpecialtyDto;
import com.hellodoc.entity.Specialty;
import com.hellodoc.security.AccountDetailsService;
import com.hellodoc.service.SpecialtyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminSpecialtyController.class)
@Import(SecurityConfig.class)
class AdminSpecialtySecurityTest {
 @Autowired MockMvc mvc; @MockBean SpecialtyService specialties; @MockBean AccountDetailsService accounts;
 @Test void anonymousCannotReadOrCreate() throws Exception {mvc.perform(get("/api/admin/specialties")).andExpect(status().isUnauthorized());mvc.perform(post("/api/admin/specialties").with(csrf()).contentType("application/json").content("{\"name\":\"Neurologie\"}")).andExpect(status().isUnauthorized());}
 @Test @WithMockUser(roles="USER") void normalUserCannotManage() throws Exception {mvc.perform(get("/api/admin/specialties")).andExpect(status().isForbidden());mvc.perform(put("/api/admin/specialties/"+UUID.randomUUID()).with(csrf()).contentType("application/json").content("{\"name\":\"Neurologie\"}")).andExpect(status().isForbidden());mvc.perform(delete("/api/admin/specialties/"+UUID.randomUUID()).with(csrf())).andExpect(status().isForbidden());}
 @Test @WithMockUser(roles="ADMIN") void adminCanListCreateAndBlankNameIsRejected() throws Exception {when(specialties.list()).thenReturn(List.of(SpecialtyDto.from(new Specialty("Cardiologie"))));when(specialties.create("Neurologie")).thenReturn(new Specialty("Neurologie"));mvc.perform(get("/api/admin/specialties")).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Cardiologie"));mvc.perform(post("/api/admin/specialties").with(csrf()).contentType("application/json").content("{\"name\":\"Neurologie\"}")).andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("Neurologie"));mvc.perform(post("/api/admin/specialties").with(csrf()).contentType("application/json").content("{\"name\":\"   \"}")).andExpect(status().isBadRequest());verify(specialties).create("Neurologie");}
}
