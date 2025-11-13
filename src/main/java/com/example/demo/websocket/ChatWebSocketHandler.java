package com.example.demo.websocket;

import com.example.demo.model.ChatMessage;
import com.example.demo.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final ChatService chatService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        String userId = extractUserIdFromUri(session);
        if (userId != null && !userId.isEmpty()) {
            sessionUserMap.put(session.getId(), userId);
            chatService.registerUserSession(userId, session);
            logger.info("Connection established. sessionId={}, userId={}, remote={}", session.getId(), userId, session.getRemoteAddress());
        } else {
            logger.warn("Connection rejected (missing userId). sessionId={}, remote={}", session.getId(), session.getRemoteAddress());
            session.close(CloseStatus.POLICY_VIOLATION);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        String userId = sessionUserMap.get(session.getId());
        if (userId == null) {
            logger.debug("Received message for unknown session: sessionId={}", session.getId());
            return;
        }

        logger.debug("Received message payload from userId={} sessionId={}: {}", userId, session.getId(), message.getPayload());
        ChatMessage chatMsg = objectMapper.readValue(message.getPayload(), ChatMessage.class);
        chatMsg.setSender(userId);
        
        if (chatMsg.getReceiver() != null && !chatMsg.getReceiver().isEmpty()) {
            logger.info("Routing private message from {} to {}: {}", userId, chatMsg.getReceiver(), chatMsg.getContent());
            try {
                chatService.sendPrivateMessage(chatMsg);
                logger.debug("Private message sent from {} to {}", userId, chatMsg.getReceiver());
            } catch (IOException e) {
                logger.error("Failed to send private message from {} to {}: {}", userId, chatMsg.getReceiver(), e.getMessage());
                throw e;
            }
        } else {
            logger.info("Broadcasting message from {} to all: {}", userId, chatMsg.getContent());
            try {
                chatService.broadcastMessage(chatMsg);
                logger.debug("Broadcast complete for message from {}", userId);
            } catch (IOException e) {
                logger.error("Failed to broadcast message from {}: {}", userId, e.getMessage());
                throw e;
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = sessionUserMap.remove(session.getId());
        if (userId != null) {
            chatService.unregisterUserSession(userId);
            logger.info("Connection closed. sessionId={}, userId={}, status={}", session.getId(), userId, status);
        } else {
            logger.info("Connection closed for unknown session. sessionId={}, status={}", session.getId(), status);
        }
    }

    private String extractUserIdFromUri(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.contains("userId=")) {
            return query.split("userId=")[1].split("&")[0];
        }
        return null;
    }
}
