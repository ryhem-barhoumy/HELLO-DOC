package com.hellodoc.controller;

import com.hellodoc.config.SecurityConfig;
import com.hellodoc.entity.UserAccount;
import com.hellodoc.entity.UserRole;
import com.hellodoc.repository.UserRepository;
import com.hellodoc.security.AccountDetailsService;
import com.hellodoc.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class PatientAuthenticationApiCompatibilityTest {
    @Autowired MockMvc mvc;
    @MockBean AuthenticationManager authentication;
    @MockBean UserRepository users;
    @MockBean AccountService accounts;
    @MockBean PasswordEncoder passwords;
    @MockBean AccountDetailsService accountDetails;

    @Test
    void csrfBootstrapAndPatientLoginKeepTheirExistingContract() throws Exception {
        UserAccount patient=new UserAccount("Pat","Ient","patient@example.test","hash",UserRole.USER,"ACTIVE");
        ReflectionTestUtils.setField(patient,"id",UUID.randomUUID());
        when(authentication.authenticate(any())).thenReturn(UsernamePasswordAuthenticationToken.authenticated(
                patient.getEmail(),null,List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        when(users.findByEmailIgnoreCase(patient.getEmail())).thenReturn(Optional.of(patient));

        mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.token").isNotEmpty());
        mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json")
                        .content("{\"email\":\"patient@example.test\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email").value("patient@example.test"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        verify(authentication).authenticate(any());
    }

    @Test
    void loginRemainsPublicButRequiresTheExistingCsrfProtection() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content("{\"email\":\"patient@example.test\",\"password\":\"secret123\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(authentication,users,accounts);
    }
}
