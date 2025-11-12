package com.example.demo.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import java.util.Map;

public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request);
        if (token != null && validateToken(token)) {
            String userId = extractUserIdFromToken(token);
            attributes.put("userId", userId);
            return true;
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                              WebSocketHandler wsHandler, Exception exception) {}

    private String extractToken(ServerHttpRequest request) {
        String query = request.getURI().getQuery();
        if (query != null && query.contains("token=")) {
            return query.split("token=")[1];
        }
        return null;
    }

    private boolean validateToken(String token) {
        // TODO: Implement JWT validation using jjwt or java-jwt
        // Compare with main-backend secret/public key
        return true; // Placeholder
    }

    private String extractUserIdFromToken(String token) {
        // TODO: Extract userId claim from JWT
        return "user123"; // Placeholder
    }
}
