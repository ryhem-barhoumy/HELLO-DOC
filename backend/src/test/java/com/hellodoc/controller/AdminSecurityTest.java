package com.hellodoc.controller;

import com.hellodoc.config.SecurityConfig;
import com.hellodoc.entity.UserRole;
import com.hellodoc.entity.UserAccount;
import com.hellodoc.entity.DoctorApplication;
import com.hellodoc.repository.UserRepository;
import com.hellodoc.repository.DoctorApplicationRepository;
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

 @Test void anonymousRequestGets401() throws Exception { mvc.perform(get("/api/admin/dashboard")).andExpect(status().isUnauthorized()); }
 @Test @WithMockUser(roles="USER") void normalUserGets403() throws Exception { mvc.perform(get("/api/admin/dashboard")).andExpect(status().isForbidden()); }
 @Test @WithMockUser(roles="ADMIN") void adminMayReadDashboard() throws Exception {
  when(users.countByRoleAndStatus(UserRole.USER,"ACTIVE")).thenReturn(2L); when(users.countByRole(UserRole.PATIENT)).thenReturn(0L); when(users.countByRole(UserRole.DOCTOR)).thenReturn(1L); when(applications.countByStatus("PENDING")).thenReturn(1L);
  mvc.perform(get("/api/admin/dashboard")).andExpect(status().isOk());
 }
 @Test @WithMockUser(roles="ADMIN") void adminApprovalPromotesPersistedPendingAccount() throws Exception {
  UUID id=UUID.randomUUID(); DoctorApplication application=mock(DoctorApplication.class); UserAccount user=mock(UserAccount.class);
  when(applications.findById(id)).thenReturn(Optional.of(application)); when(application.getStatus()).thenReturn("PENDING"); when(application.getUser()).thenReturn(user);
  when(application.getId()).thenReturn(id); when(user.getFirstName()).thenReturn("Doctor"); when(user.getLastName()).thenReturn("Example");
  when(application.getServiceTitle()).thenReturn("Dr Example"); when(application.getSpecialty()).thenReturn("Cardiology"); when(application.getLicenseNumber()).thenReturn("LIC-1");
  when(application.getCity()).thenReturn("Tunis"); when(application.getPracticeAddress()).thenReturn("Clinic");
  doAnswer(invocation->{when(application.getStatus()).thenReturn((String)invocation.getArgument(0));return null;}).when(application).decide(any(),any());
  mvc.perform(post("/api/admin/doctors/"+id+"/approve").with(csrf()).contentType("application/json").content("{}"))
   .andExpect(status().isOk());
  verify(user).setRoleAndStatus(UserRole.DOCTOR,"APPROVED"); verify(application).decide(eq("APPROVED"),eq(null));
 }
}
