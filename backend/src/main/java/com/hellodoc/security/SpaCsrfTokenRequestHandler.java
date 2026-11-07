package com.hellodoc.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import java.util.function.Supplier;

/** Accepts Angular's raw XSRF cookie/header pair while preserving masked request attributes. */
public final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
 private final CsrfTokenRequestHandler plain=new CsrfTokenRequestAttributeHandler();
 private final CsrfTokenRequestHandler xor=new XorCsrfTokenRequestAttributeHandler();
 @Override public void handle(HttpServletRequest request,HttpServletResponse response,Supplier<CsrfToken> deferredToken){
  xor.handle(request,response,deferredToken);
  deferredToken.get(); // Force CookieCsrfTokenRepository to issue the XSRF-TOKEN cookie.
 }
 @Override public String resolveCsrfTokenValue(HttpServletRequest request,CsrfToken token){
  String header=request.getHeader(token.getHeaderName());
  return header!=null&&!header.isBlank()?plain.resolveCsrfTokenValue(request,token):xor.resolveCsrfTokenValue(request,token);
 }
}
