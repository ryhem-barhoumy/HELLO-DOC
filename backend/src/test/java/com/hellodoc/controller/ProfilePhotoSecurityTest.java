package com.hellodoc.controller;
import com.hellodoc.config.SecurityConfig;
import com.hellodoc.security.AccountDetailsService;
import com.hellodoc.service.ProfilePhotoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(ProfilePhotoController.class) @Import(SecurityConfig.class)
class ProfilePhotoSecurityTest {
 @Autowired MockMvc mvc;@MockBean ProfilePhotoService photos;@MockBean AccountDetailsService accounts;
 @Test void privatePhotosRequireAuthentication()throws Exception{mvc.perform(get("/api/account/me/photo")).andExpect(status().isUnauthorized());verifyNoInteractions(photos);}
 @Test void publicEndpointOnlyExposesTheDoctorsPhoto()throws Exception{UUID id=UUID.randomUUID();when(photos.doctorPhoto(id)).thenReturn(new byte[]{1,2});mvc.perform(get("/api/doctors/"+id+"/photo?v=test")).andExpect(status().isOk()).andExpect(content().contentType("image/jpeg")).andExpect(header().string("Cache-Control","no-store"));verify(photos).doctorPhoto(id);}
 @Test void photoWritesRequireCsrfAndUseTheAuthenticatedIdentity()throws Exception{var file=new MockMultipartFile("photo","portrait.jpg","image/jpeg",new byte[]{1});mvc.perform(multipart("/api/account/me/photo").file(file).with(user("patient").roles("USER"))).andExpect(status().isForbidden());verifyNoInteractions(photos);when(photos.replace(eq("patient"),any())).thenReturn("/api/account/me/photo?v=1");mvc.perform(multipart("/api/account/me/photo").file(file).with(user("patient").roles("USER")).with(csrf())).andExpect(status().isOk());verify(photos).replace(eq("patient"),any());}
 @Test void removalUsesTheSessionIdentityAndRequiresCsrf()throws Exception{mvc.perform(delete("/api/account/me/photo").with(user("doctor").roles("DOCTOR"))).andExpect(status().isForbidden());mvc.perform(delete("/api/account/me/photo").with(user("doctor").roles("DOCTOR")).with(csrf())).andExpect(status().isNoContent());verify(photos).remove("doctor");}
}
