package com.agenticai.client;

import com.agenticai.service.AgentService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.util.Map;
import java.util.concurrent.CompletionStage;

@Component
public class MCPClientWebSocket {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private WebSocket webSocket;
    private final String mcpServerUrl = "ws://localhost:8080/mcp";

    private final AgentService agentService;

    // Inject AgentService lazily to break circular dependency
    public MCPClientWebSocket(@Lazy AgentService agentService) {
        this.agentService = agentService;
    }

    @PostConstruct
    public void init() {
        connect();
    }

    private void connect() {
        System.out.println("[MCPClientWebSocket] Connecting to " + mcpServerUrl + " ...");
        try {
            HttpClient client = HttpClient.newHttpClient();
            webSocket = client.newWebSocketBuilder()
                    .buildAsync(URI.create(mcpServerUrl), new MCPWebSocketListener())
                    .join();
        } catch (Exception e) {
            System.err.println("[MCPClientWebSocket] Connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendMessage(String payload) {
        if (webSocket != null) {
            webSocket.sendText(payload, true)
                    .exceptionally(ex -> {
                        System.err.println("[MCPClientWebSocket] Failed to send message: " + ex.getMessage());
                        return null;
                    });
        } else {
            System.err.println("[MCPClientWebSocket] WebSocket not connected. Cannot send message.");
        }
    }

    private class MCPWebSocketListener implements Listener {

        @Override
        public void onOpen(WebSocket ws) {
            System.out.println("[MCPClientWebSocket] Connected to MCP server");
            Listener.super.onOpen(ws);
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            String msg = data.toString();
            System.out.println("[MCPClientWebSocket] Received message: " + msg);

            try {
                Map<String, Object> messageMap = objectMapper.readValue(msg, new TypeReference<Map<String, Object>>() {});
                String taskId = messageMap.getOrDefault("taskId", "").toString();

                // Forward to AgentService for handling
                agentService.handleMCPResponse(msg);
            } catch (Exception e) {
                System.out.println("[MCPClientWebSocket] Non-JSON message received: " + msg);
            }

            return Listener.super.onText(ws, data, last);
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            System.err.println("[MCPClientWebSocket] WebSocket error: " + error.getMessage());
            Listener.super.onError(ws, error);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            System.out.println("[MCPClientWebSocket] WebSocket closed: " + statusCode + " - " + reason);
            return Listener.super.onClose(ws, statusCode, reason);
        }
    }
}
