package com.hellodoc.config;

import com.hellodoc.security.AccountDetailsService;
import com.hellodoc.security.SpaCsrfTokenRequestHandler;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
 @Bean AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception{return config.getAuthenticationManager();}
 @Bean SecurityFilterChain security(HttpSecurity http, AccountDetailsService users) throws Exception {
  http.userDetailsService(users)
   .cors(cors->{})
   .csrf(csrf->csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()).csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
   .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
   .formLogin(form->form.disable()).httpBasic(basic->basic.disable())
   .authorizeHttpRequests(auth->auth
    .requestMatchers(HttpMethod.OPTIONS,"/**").permitAll()
    .requestMatchers(HttpMethod.GET,"/api/auth/csrf","/api/auth/me").permitAll()
    .requestMatchers(HttpMethod.POST,"/api/auth/login","/api/patients/registrations","/api/doctors/registrations").permitAll()
    .requestMatchers("/api/admin/**").hasRole("ADMIN")
    .requestMatchers("/api/**").authenticated()
    .anyRequest().permitAll())
   .exceptionHandling(ex->ex.authenticationEntryPoint((request,response,error)->response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
    .accessDeniedHandler((request,response,error)->response.sendError(HttpServletResponse.SC_FORBIDDEN)));
  return http.build();
 }
 @Bean CorsConfigurationSource corsConfigurationSource(@Value("${hellodoc.frontend-origin}") String origin){
  CorsConfiguration config=new CorsConfiguration(); config.setAllowedOrigins(List.of(origin));
  config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
  config.setAllowedHeaders(List.of("Content-Type","X-XSRF-TOKEN","X-Requested-With")); config.setAllowCredentials(true);
  UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource(); source.registerCorsConfiguration("/api/**",config); return source;
 }
}
