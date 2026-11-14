package com.hellodoc.controller;

import com.hellodoc.config.SecurityConfig;
import com.hellodoc.entity.UserRole;
import com.hellodoc.entity.UserAccount;
import com.hellodoc.entity.DoctorApplication;
import com.hellodoc.repository.UserRepository;
import com.hellodoc.repository.DoctorApplicationRepository;
import com.hellodoc.service.DoctorApprovalService;
import com.hellodoc.service.AdminDirectoryService;
import com.hellodoc.dto.DoctorApplicationSummary;
import com.hellodoc.security.AccountDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import java.util.Optional;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminSecurityTest {
 @Autowired MockMvc mvc;
 @MockBean UserRepository users;
 @MockBean DoctorApplicationRepository applications;
 @MockBean AccountDetailsService accountDetails;
 @MockBean DoctorApprovalService approvals;
 @MockBean AdminDirectoryService directory;
 @MockBean com.hellodoc.service.AdminProfileService profiles;

 @Test void anonymousRequestGets401() throws Exception { mvc.perform(get("/api/admin/dashboard")).andExpect(status().isUnauthorized()); }
 @Test @WithMockUser(roles="USER") void normalUserGets403() throws Exception { mvc.perform(get("/api/admin/dashboard")).andExpect(status().isForbidden()); }
 @Test @WithMockUser(roles="ADMIN") void adminMayReadDashboard() throws Exception {
  when(users.countByRoleAndStatus(UserRole.USER,"ACTIVE")).thenReturn(2L); when(users.countByRole(UserRole.PATIENT)).thenReturn(0L); when(users.countByRole(UserRole.DOCTOR)).thenReturn(1L); when(applications.countByStatus("PENDING")).thenReturn(1L);
  mvc.perform(get("/api/admin/dashboard")).andExpect(status().isOk());
 }
 @Test @WithMockUser(roles="ADMIN") void adminDirectoryEndpointsReadPersistedDirectories() throws Exception {
  when(directory.doctors(any(int.class),any(int.class))).thenReturn(org.springframework.data.domain.Page.empty());
  when(directory.patients(any(int.class),any(int.class))).thenReturn(org.springframework.data.domain.Page.empty());
  mvc.perform(get("/api/admin/doctors/directory")).andExpect(status().isOk());
  mvc.perform(get("/api/admin/patients")).andExpect(status().isOk());
 }
 @Test @WithMockUser(roles="USER") void normalUserCannotReadAdminDirectories() throws Exception {
  mvc.perform(get("/api/admin/doctors/directory")).andExpect(status().isForbidden());
  mvc.perform(get("/api/admin/patients")).andExpect(status().isForbidden());
 }
 @Test @WithMockUser(roles="ADMIN") void adminApprovalPromotesPersistedPendingAccount() throws Exception {
  UUID id=UUID.randomUUID();when(approvals.approve(eq(id),any())).thenReturn(new DoctorApplicationSummary(id,"Doctor","Example","d@example.com","555","Doctor Example","Doctor Example","Dr Example","Cardiology","LIC-1","Tunis","Clinic","Clinic","Home","APPROVED","",true));
  mvc.perform(post("/api/admin/doctors/"+id+"/approve").with(csrf()).contentType("application/json").content("{}"))
   .andExpect(status().isOk());
  verify(approvals).approve(eq(id),any());
 }
}
