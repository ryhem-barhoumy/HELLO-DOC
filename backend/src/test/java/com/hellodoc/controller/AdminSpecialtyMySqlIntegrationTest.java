package com.hellodoc.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellodoc.repository.SpecialtyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIf(expression = "#{systemProperties['hellodoc.mysql.integration'] == 'true'}", loadContext = false)
class AdminSpecialtyMySqlIntegrationTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper mapper; @Autowired SpecialtyRepository specialties;
 UUID createdId;
 @AfterEach void cleanup(){if(createdId!=null&&specialties.existsById(createdId))specialties.deleteById(createdId);}
 @Test @WithMockUser(roles="ADMIN") void createUpdateListAndDeletePersistInMySql() throws Exception {
  String original="QA Spécialité "+UUID.randomUUID(); String renamed=original+" modifiée";
  MvcResult created=mvc.perform(post("/api/admin/specialties").with(csrf()).contentType("application/json").content(mapper.writeValueAsBytes(java.util.Map.of("name",original))))
   .andExpect(status().isCreated()).andReturn();
  createdId=UUID.fromString(mapper.readTree(created.getResponse().getContentAsString()).get("id").asText());
  assertTrue(specialties.findById(createdId).isPresent(),"new specialty must be committed to MySQL");
  mvc.perform(post("/api/admin/specialties").with(csrf()).contentType("application/json").content(mapper.writeValueAsBytes(java.util.Map.of("name",original.toUpperCase())))).andExpect(status().isConflict());
  mvc.perform(put("/api/admin/specialties/"+createdId).with(csrf()).contentType("application/json").content(mapper.writeValueAsBytes(java.util.Map.of("name",renamed))))
   .andExpect(status().isOk());
  assertEquals(renamed,specialties.findById(createdId).orElseThrow().getName());
  mvc.perform(get("/api/admin/specialties")).andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString(renamed)));
  mvc.perform(delete("/api/admin/specialties/"+createdId).with(csrf())).andExpect(status().isNoContent());
  assertFalse(specialties.existsById(createdId),"deleted specialty must remain absent from MySQL"); createdId=null;
 }
 @Test @WithMockUser(roles="ADMIN") void deleteOfSpecialtyUsedByExistingDoctorIsRejected() throws Exception {
  var used=specialties.findByNormalizedName("pediatrie").orElseThrow(()->new AssertionError("fixture expects the existing doctor specialty"));
  mvc.perform(delete("/api/admin/specialties/"+used.getId()).with(csrf())).andExpect(status().isConflict());
  assertTrue(specialties.existsById(used.getId()),"used specialty must remain in MySQL");
 }
}
