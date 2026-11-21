package com.hellodoc.security;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellodoc.entity.*;
import com.hellodoc.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class SessionIntegrityFilterTest {
 @AfterEach void clean(){SecurityContextHolder.clearContext();}
 private void verifyAccount(String status,UserRole role,boolean expected)throws Exception{
  var users=mock(UserRepository.class);when(users.findByEmailIgnoreCase("doctor@test.local")).thenReturn(Optional.of(new UserAccount("Test","Doctor","doctor@test.local","hash",role,status)));
  SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("doctor@test.local","",List.of(new SimpleGrantedAuthority("ROLE_DOCTOR"))));
  var request=new MockHttpServletRequest("GET","/api/doctors/me");var session=(MockHttpSession)request.getSession();var response=new MockHttpServletResponse();var called=new AtomicBoolean();
  new SessionIntegrityFilter(users,new ObjectMapper().findAndRegisterModules()).doFilter(request,response,(req,res)->called.set(true));
  assertEquals(expected,called.get());if(!expected){assertTrue(session.isInvalid());assertNull(SecurityContextHolder.getContext().getAuthentication());assertEquals(401,response.getStatus());assertTrue(response.getContentAsString().contains("SESSION_REVOKED"));}
 }
 @Test void activeAccountContinues()throws Exception{verifyAccount("ACTIVE",UserRole.DOCTOR,true);}
 @Test void deactivatedAccountLosesAnAlreadyEstablishedSession()throws Exception{verifyAccount("INACTIVE",UserRole.DOCTOR,false);}
 @Test void changedRoleCannotKeepPreviousAuthorities()throws Exception{verifyAccount("ACTIVE",UserRole.USER,false);}
 @Test void publicRequestDoesNotRequireAnAccount()throws Exception{var users=mock(UserRepository.class);var called=new AtomicBoolean();new SessionIntegrityFilter(users,new ObjectMapper().findAndRegisterModules()).doFilter(new MockHttpServletRequest("GET","/api/specialties"),new MockHttpServletResponse(),(req,res)->called.set(true));assertTrue(called.get());verifyNoInteractions(users);}
}
