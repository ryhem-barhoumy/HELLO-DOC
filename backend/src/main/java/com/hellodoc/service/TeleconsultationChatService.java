package com.hellodoc.service;
import com.hellodoc.dto.TeleconsultationMessageDto;
import com.hellodoc.entity.*;
import com.hellodoc.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
@Service public class TeleconsultationChatService{
 private final TeleconsultationService sessions; private final TeleconsultationSessionRepository sessionRepo; private final TeleconsultationMessageRepository messages; private final UserRepository users;
 public TeleconsultationChatService(TeleconsultationService sessions,TeleconsultationSessionRepository sessionRepo,TeleconsultationMessageRepository messages,UserRepository users){this.sessions=sessions;this.sessionRepo=sessionRepo;this.messages=messages;this.users=users;}
 @Transactional(readOnly=true) public List<TeleconsultationMessageDto> history(String email,UUID id){var s=sessions.authorized(email,id);return messages.findTop200BySession_IdOrderBySentAtAsc(id).stream().map(TeleconsultationMessageDto::from).toList();}
 @Transactional public TeleconsultationMessageDto send(String email,UUID id,String body){var s=sessions.authorized(email,id);if(s.getState()==TeleconsultationSession.State.COMPLETED)throw new ResponseStatusException(HttpStatus.CONFLICT,"La séance est terminée.");if(body==null||body.isBlank()||body.length()>2000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Le message doit contenir de 1 à 2 000 caractères.");UserAccount user=users.findByEmailIgnoreCase(email).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));return TeleconsultationMessageDto.from(messages.save(new TeleconsultationMessage(s,user,body)));}
}
