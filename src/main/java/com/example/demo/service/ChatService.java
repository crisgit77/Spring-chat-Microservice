package com.example.demo.service;

import com.example.demo.model.ChatMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class ChatService {
    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, Set<String>> chatRooms = new HashMap<>();

    public ChatService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void addUserToChat(String chatId, String userId) {
        chatRooms.computeIfAbsent(chatId, k -> new HashSet<>()).add(userId);
    }

    public void removeUserFromChat(String chatId, String userId) {
        Set<String> users = chatRooms.get(chatId);
        if (users != null) {
            users.remove(userId);
            if (users.isEmpty()) {
                chatRooms.remove(chatId);
            }
        }
    }

    public void sendPrivateMessage(ChatMessage message) {
        String receiver = message.getReceiver();
        messagingTemplate.convertAndSendToUser(receiver, "/queue/messages", message);
    }

    public void broadcastMessage(String chatId, ChatMessage message) {
        messagingTemplate.convertAndSend("/topic/" + chatId, message);
    }

    private String generateChatId(String user1, String user2) {
        return user1.compareTo(user2) < 0 ? user1 + "_" + user2 : user2 + "_" + user1;
    }
}
