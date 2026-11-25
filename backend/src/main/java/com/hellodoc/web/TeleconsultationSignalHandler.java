package com.hellodoc.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hellodoc.entity.TeleconsultationSession;
import com.hellodoc.service.TeleconsultationService;
import com.hellodoc.service.TeleconsultationChatService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Authenticated, participant-checked WebRTC signaling relay. No network disconnect changes business state. */
@Component
public class TeleconsultationSignalHandler extends TextWebSocketHandler {
    private final ObjectMapper mapper; private final TeleconsultationService service; private final TeleconsultationChatService chat;
    private final ConcurrentHashMap<UUID,Set<WebSocketSession>> rooms=new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID,JsonNode> pendingOffers=new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID,java.util.List<JsonNode>> pendingSignals=new ConcurrentHashMap<>();
    public TeleconsultationSignalHandler(ObjectMapper mapper,TeleconsultationService service,TeleconsultationChatService chat){this.mapper=mapper;this.service=service;this.chat=chat;}
    @Override public void afterConnectionEstablished(WebSocketSession socket)throws Exception{
        if(socket.getPrincipal()==null){socket.close(CloseStatus.NOT_ACCEPTABLE.withReason("Authentication required"));return;}
    }
    @Override protected void handleTextMessage(WebSocketSession socket,TextMessage message)throws Exception{
        if(message.getPayloadLength()>64_000){socket.close(CloseStatus.TOO_BIG_TO_PROCESS);return;}
        try{
            JsonNode input=mapper.readTree(message.getPayload());UUID id=UUID.fromString(input.path("sessionId").asText());
            String type=input.path("type").asText();if(!Set.of("JOIN","OFFER","ANSWER","ICE","CHAT").contains(type))throw new IllegalArgumentException("Unsupported signal type");
            TeleconsultationSession session=service.authorized(socket.getPrincipal().getName(),id);
            if(session.getState()!=TeleconsultationSession.State.ACTIVE)throw new IllegalStateException("Session not active");
            rooms.computeIfAbsent(id,key->ConcurrentHashMap.newKeySet()).add(socket);socket.getAttributes().put("teleconsultationId",id);
            if(type.equals("JOIN")){Set<WebSocketSession> room=rooms.get(id);if(room.size()>1){JsonNode offer=pendingOffers.get(id);if(offer!=null&&socket.isOpen())socket.sendMessage(new TextMessage(mapper.writeValueAsString(offer)));for(JsonNode signal:pendingSignals.getOrDefault(id,java.util.List.of()))if(socket.isOpen())socket.sendMessage(new TextMessage(mapper.writeValueAsString(signal)));pendingSignals.remove(id);}return;}
            ObjectNode output=mapper.createObjectNode().put("type",type);output.put("sessionId",id.toString());
            if(type.equals("CHAT")){var saved=chat.send(socket.getPrincipal().getName(),id,input.path("payload").path("body").asText());output.set("payload",mapper.valueToTree(saved));}
            else output.set("payload",input.path("payload"));
            if(type.equals("OFFER"))pendingOffers.put(id,output);if(type.equals("ANSWER"))pendingOffers.remove(id);
            Set<WebSocketSession> room=rooms.get(id);if(type.equals("ICE")&&room.size()<2){pendingSignals.computeIfAbsent(id,key->new java.util.concurrent.CopyOnWriteArrayList<>()).add(output);return;}
            synchronized(room){for(WebSocketSession peer:room)if((type.equals("CHAT")||!peer.getId().equals(socket.getId()))&&peer.isOpen())peer.sendMessage(new TextMessage(mapper.writeValueAsString(output)));}
        }catch(Exception error){
            if(socket.isOpen())socket.sendMessage(new TextMessage(mapper.writeValueAsString(java.util.Map.of("type","ERROR","message","Signal refusé ou session indisponible."))));
        }
    }
    @Override public void afterConnectionClosed(WebSocketSession socket,CloseStatus status){Object id=socket.getAttributes().get("teleconsultationId");if(id instanceof UUID sessionId){Set<WebSocketSession> peers=rooms.get(sessionId);if(peers!=null){peers.remove(socket);if(peers.isEmpty())rooms.remove(sessionId,peers);}}}
}
