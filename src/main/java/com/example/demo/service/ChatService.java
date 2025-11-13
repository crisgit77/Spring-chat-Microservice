package com.example.demo.service;

import com.example.demo.model.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatService {
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> chatRooms = new ConcurrentHashMap<>();

    public void registerUserSession(String userId, WebSocketSession session) {
        userSessions.put(userId, session);
        logger.info("Registered session for userId={} sessionId={}", userId, session.getId());
        logContainers();
    }

    public void unregisterUserSession(String userId) {
        userSessions.remove(userId);
        logger.info("Unregistered session for userId={}", userId);
        logContainers();
    }

    public void joinChatRoom(String chatRoomId, String userId) {
        chatRooms.computeIfAbsent(chatRoomId, k -> ConcurrentHashMap.newKeySet()).add(userId);
        logger.info("User {} joined chatRoom {}", userId, chatRoomId);
        logContainers();
    }

    public void leaveChatRoom(String chatRoomId, String userId) {
        Set<String> users = chatRooms.get(chatRoomId);
        if (users != null) {
            users.remove(userId);
            if (users.isEmpty()) {
                chatRooms.remove(chatRoomId);
            }
        }
        logger.info("User {} left chatRoom {}", userId, chatRoomId);
        logContainers();
    }

    public void sendPrivateMessage(ChatMessage message) throws IOException {
        String receiver = message.getReceiver();
        WebSocketSession session = userSessions.get(receiver);
        if (session != null && session.isOpen()) {
            String payload = objectMapper.writeValueAsString(message);
            logger.debug("Sending private message to {} payload={}", receiver, payload);
            session.sendMessage(new TextMessage(payload));
            logger.info("Private message delivered to {}", receiver);
        } else {
            logger.warn("Cannot deliver private message to {} - session not available or closed", receiver);
        }
    }

    public void broadcastMessage(ChatMessage message) throws IOException {
        String payload = objectMapper.writeValueAsString(message);
        int sent = 0;
        for (WebSocketSession session : userSessions.values()) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(payload));
                sent++;
            }
        }
        logger.info("Broadcasted message from {} to {} sessions", message.getSender(), sent);
    }

    public void broadcastToChatRoom(String chatRoomId, ChatMessage message) throws IOException {
        Set<String> users = chatRooms.get(chatRoomId);
        if (users == null) {
            logger.debug("No users in chatRoom {} to broadcast", chatRoomId);
            return;
        }
        String payload = objectMapper.writeValueAsString(message);
        int sent = 0;
        for (String userId : users) {
            WebSocketSession session = userSessions.get(userId);
            if (session != null && session.isOpen()) {
                session.sendMessage(new TextMessage(payload));
                sent++;
            }
        }
        logger.info("Broadcasted message to chatRoom {}: {} deliveries", chatRoomId, sent);
    }

    private void logContainers() {
        logger.debug("=== CONTAINER STATE ===");
        logger.debug("Active user sessions: {}", userSessions.size());
        for (String userId : userSessions.keySet()) {
            WebSocketSession session = userSessions.get(userId);
            logger.debug("  - userId: {}, sessionId: {}, isOpen: {}", userId, session.getId(), session.isOpen());
        }
        
        logger.debug("Active chatRooms: {}", chatRooms.size());
        for (String chatRoomId : chatRooms.keySet()) {
            Set<String> users = chatRooms.get(chatRoomId);
            logger.debug("  - chatRoomId: {}, users: {}", chatRoomId, users);
        }
        logger.debug("=== END CONTAINER STATE ===");
    }
}
