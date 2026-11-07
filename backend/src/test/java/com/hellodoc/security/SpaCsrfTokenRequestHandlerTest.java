package com.hellodoc.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SpaCsrfTokenRequestHandlerTest {
 @Test void acceptsTheRawCookieTokenForwardedByAngularAsHeader(){
  var request=new MockHttpServletRequest(); request.addHeader("X-XSRF-TOKEN","raw-cookie-token");
  var csrf=new DefaultCsrfToken("X-XSRF-TOKEN","_csrf","raw-cookie-token");
  assertEquals("raw-cookie-token",new SpaCsrfTokenRequestHandler().resolveCsrfTokenValue(request,csrf));
 }
}
