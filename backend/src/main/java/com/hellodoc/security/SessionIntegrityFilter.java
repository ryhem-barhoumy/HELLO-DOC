package com.hellodoc.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hellodoc.dto.ApiError;
import com.hellodoc.repository.UserRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Map;

/** Rechecks the account because a session can outlive an administrator's decision. */
public class SessionIntegrityFilter extends OncePerRequestFilter {
    private final UserRepository users;
    private final ObjectMapper mapper;
    public SessionIntegrityFilter(UserRepository users,ObjectMapper mapper){this.users=users;this.mapper=mapper;}
    @Override protected boolean shouldNotFilter(HttpServletRequest request){return !request.getRequestURI().startsWith("/api/")||request.getRequestURI().equals("/api/auth/logout");}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
        var authentication=SecurityContextHolder.getContext().getAuthentication();
        if(authentication!=null&&authentication.isAuthenticated()&&!(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)){
            boolean valid=users.findByEmailIgnoreCase(authentication.getName()).map(u->"ACTIVE".equals(u.getStatus())&&authentication.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_"+u.getRole().name()))).orElse(false);
            if(!valid){
                SecurityContextHolder.clearContext();var session=request.getSession(false);if(session!=null)session.invalidate();
                response.setStatus(401);response.setCharacterEncoding("UTF-8");response.setContentType("application/json");
                mapper.writeValue(response.getOutputStream(),ApiError.of(401,"SESSION_REVOKED","Votre compte ou vos autorisations ont changé. Connectez-vous à nouveau.",Map.of()));return;
            }
        }
        chain.doFilter(request,response);
    }
}
