package com.hellodoc.config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellodoc.repository.UserRepository;
import com.hellodoc.security.SessionIntegrityFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
@Configuration
public class SessionIntegrityConfiguration {
 @Bean SessionIntegrityFilter sessionIntegrityFilter(UserRepository users,ObjectMapper mapper){return new SessionIntegrityFilter(users,mapper);}
 @Bean FilterRegistrationBean<SessionIntegrityFilter> sessionIntegrityRegistration(SessionIntegrityFilter filter){var registration=new FilterRegistrationBean<>(filter);registration.setEnabled(false);return registration;}
}
