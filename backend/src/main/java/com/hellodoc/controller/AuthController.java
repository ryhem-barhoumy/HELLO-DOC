package com.hellodoc.controller;

import com.hellodoc.dto.LoginRequest;
import com.hellodoc.dto.ChangePasswordRequest;
import com.hellodoc.dto.DoctorRegistrationRequest;
import com.hellodoc.dto.RegistrationRequest;
import com.hellodoc.dto.SessionUser;
import com.hellodoc.entity.UserAccount;
import com.hellodoc.repository.UserRepository;
import com.hellodoc.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {
 private final AuthenticationManager authentication; private final UserRepository users; private final AccountService accounts; private final PasswordEncoder passwords;
 public AuthController(AuthenticationManager authentication,UserRepository users,AccountService accounts,PasswordEncoder passwords){this.authentication=authentication;this.users=users;this.accounts=accounts;this.passwords=passwords;}
 @GetMapping("/auth/csrf") public Map<String,String> csrf(CsrfToken token){return Map.of("headerName",token.getHeaderName(),"token",token.getToken());}
 @PostMapping("/auth/login") public SessionUser login(@Valid @RequestBody LoginRequest body,HttpServletRequest request){
  Authentication result;
  try {result=authentication.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(body.email().trim().toLowerCase(),body.password()));}
  catch (LockedException e){throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN,"Doctor registration is awaiting administrator approval.");}
  catch (AuthenticationException e){throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid email or password.");}
  request.getSession(true); request.changeSessionId(); SecurityContext context=SecurityContextHolder.createEmptyContext(); context.setAuthentication(result); SecurityContextHolder.setContext(context);
  request.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,context);
  return current(result);
 }
 @GetMapping("/auth/me") public ResponseEntity<SessionUser> me(Authentication auth){return auth==null||!auth.isAuthenticated()||auth instanceof AnonymousAuthenticationToken?ResponseEntity.status(401).build():ResponseEntity.ok(current(auth));}
 @PostMapping("/auth/password") public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest body,Authentication auth){
  UserAccount user=users.findByEmailIgnoreCase(auth.getName()).orElseThrow();
  if(!passwords.matches(body.currentPassword(),user.getPasswordHash()))throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED,"Current password is incorrect.");
  user.changePassword(passwords.encode(body.newPassword())); users.save(user); return ResponseEntity.noContent().build();
 }
 @PostMapping("/auth/logout") public ResponseEntity<Void> logout(HttpServletRequest request,HttpServletResponse response){
  SecurityContextHolder.clearContext(); HttpSession session=request.getSession(false); if(session!=null)session.invalidate();
  var cookie=new jakarta.servlet.http.Cookie("JSESSIONID",""); cookie.setPath("/"); cookie.setMaxAge(0); cookie.setHttpOnly(true); response.addCookie(cookie); return ResponseEntity.noContent().build();
 }
 @PostMapping("/patients/registrations") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> registerPatient(@Valid @RequestBody RegistrationRequest body){return registrationResult(accounts.register(body));}
 @PostMapping("/doctors/registrations") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> registerDoctor(@Valid @RequestBody DoctorRegistrationRequest body){var application=accounts.registerDoctor(body);return Map.of("id",application.getId(),"status",application.getStatus());}
 private Map<String,Object> registrationResult(UserAccount u){return Map.of("id",u.getId(),"status",u.getStatus());}
 private SessionUser current(Authentication auth){return SessionUser.from(users.findByEmailIgnoreCase(auth.getName()).orElseThrow());}
}
