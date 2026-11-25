package com.hellodoc.controller;

import com.hellodoc.dto.TeleconsultationDto;
import com.hellodoc.dto.IceServerDto;
import com.hellodoc.service.TeleconsultationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

@RestController @RequestMapping("/api")
public class TeleconsultationController {
    private final TeleconsultationService service;
    public TeleconsultationController(TeleconsultationService service){this.service=service;}
    @GetMapping("/teleconsultations/{id}/ice-config") @PreAuthorize("hasAnyRole('DOCTOR','USER','PATIENT')")
    public IceServerDto iceConfig(Authentication auth,@PathVariable UUID id,@Value("${hellodoc.webrtc.stun-urls}") String stun,@Value("${hellodoc.webrtc.turn-urls:}") String turn,@Value("${hellodoc.webrtc.turn-shared-secret:}") String sharedSecret){service.get(auth.getName(),id);var servers=new java.util.ArrayList<IceServerDto.Server>();servers.add(new IceServerDto.Server(java.util.Arrays.stream(stun.split(",")).map(String::trim).filter(s->!s.isEmpty()).toList(),null,null));var turnUrls=java.util.Arrays.stream(turn.split(",")).map(String::trim).filter(s->!s.isEmpty()).toList();if(!turnUrls.isEmpty()&&!sharedSecret.isBlank())try{long expiry=java.time.Instant.now().getEpochSecond()+3600;String username=expiry+":"+auth.getName();javax.crypto.Mac mac=javax.crypto.Mac.getInstance("HmacSHA1");mac.init(new javax.crypto.spec.SecretKeySpec(sharedSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8),"HmacSHA1"));String credential=java.util.Base64.getEncoder().encodeToString(mac.doFinal(username.getBytes(java.nio.charset.StandardCharsets.UTF_8)));servers.add(new IceServerDto.Server(turnUrls,username,credential));}catch(java.security.GeneralSecurityException ex){throw new IllegalStateException("TURN credential generation failed",ex);}return new IceServerDto(servers);}
    @PostMapping("/appointments/{id}/teleconsultation") @PreAuthorize("hasRole('DOCTOR')") public TeleconsultationDto create(Authentication a,@PathVariable UUID id){return service.create(a.getName(),id);}
    @GetMapping("/appointments/{id}/teleconsultation") @PreAuthorize("hasAnyRole('DOCTOR','USER','PATIENT')") public TeleconsultationDto forAppointment(Authentication a,@PathVariable UUID id){return service.getForAppointment(a.getName(),id);}
    @GetMapping("/teleconsultations/{id}") @PreAuthorize("hasAnyRole('DOCTOR','USER','PATIENT')") public TeleconsultationDto get(Authentication a,@PathVariable UUID id){return service.get(a.getName(),id);}
    @PostMapping("/teleconsultations/{id}/start") @PreAuthorize("hasRole('DOCTOR')") public TeleconsultationDto start(Authentication a,@PathVariable UUID id){return service.start(a.getName(),id);}
    @PostMapping("/teleconsultations/{id}/complete") @PreAuthorize("hasRole('DOCTOR')") public TeleconsultationDto complete(Authentication a,@PathVariable UUID id){return service.complete(a.getName(),id);}
}
