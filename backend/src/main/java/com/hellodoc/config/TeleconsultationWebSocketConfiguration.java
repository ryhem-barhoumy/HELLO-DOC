package com.hellodoc.config;
import com.hellodoc.web.TeleconsultationSignalHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
@Configuration @EnableWebSocket
public class TeleconsultationWebSocketConfiguration implements WebSocketConfigurer{
 private final TeleconsultationSignalHandler handler; private final String frontendOrigin;
 public TeleconsultationWebSocketConfiguration(TeleconsultationSignalHandler handler,@Value("${hellodoc.frontend-origin}") String frontendOrigin){this.handler=handler;this.frontendOrigin=frontendOrigin;}
 @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry registry){registry.addHandler(handler,"/api/teleconsultation-signal").setAllowedOrigins(frontendOrigin);}
 @Bean public ServletServerContainerFactoryBean webSocketContainer(){var container=new ServletServerContainerFactoryBean();container.setMaxTextMessageBufferSize(64*1024);container.setMaxBinaryMessageBufferSize(64*1024);container.setMaxSessionIdleTimeout(10*60*1000L);return container;}
}
