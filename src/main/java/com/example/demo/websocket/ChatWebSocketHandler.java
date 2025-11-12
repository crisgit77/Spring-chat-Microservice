package com.example.demo.websocket;

import com.example.demo.model.ChatMessage;
import com.example.demo.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    
    private final ChatService chatService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        String userId = extractUserIdFromUri(session);
        if (userId != null && validateToken(session)) {
            sessions.put(userId, session);
            chatService.addUserToChat(userId, userId);
        } else {
            session.close(CloseStatus.POLICY_VIOLATION);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String userId = findUserBySession(session);
        if (userId == null) return;

        ChatMessage chatMsg = objectMapper.readValue(message.getPayload(), ChatMessage.class);
        chatMsg.setSender(userId);
        //chatService.broadcastMessage(chatMsg);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = findUserBySession(session);
        if (userId != null) {
            sessions.remove(userId);
            chatService.removeUserFromChat(userId, userId);
        }
    }

    private String extractUserIdFromUri(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("userId=")) {
            return query.split("userId=")[1];
        }
        return null;
    }

    private boolean validateToken(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("token=")) {
            String token = query.split("token=")[1];
            return true; // TODO: Implement JWT validation
        }
        return false;
    }

    private String findUserBySession(WebSocketSession session) {
        return sessions.entrySet().stream()
                .filter(e -> e.getValue() == session)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}
