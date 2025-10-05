package com.agenticai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletionStage;

@Component
public class MCPClientWebSocket {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private WebSocket webSocket;
    private final Map<String, CompletableFuture<String>> taskFutures = new ConcurrentHashMap<>();

    private final String mcpServerUrl = "ws://localhost:8080/mcp"; // keep it same as application.yml MCP server

    @PostConstruct
    public void init() {
        connect();
    }

    private void connect() {
        System.out.println("[MCPClientWebSocket] Connecting to " + mcpServerUrl + " ...");
        try {
            HttpClient client = HttpClient.newHttpClient();
            webSocket = client.newWebSocketBuilder()
                    .buildAsync(URI.create(mcpServerUrl), new WebSocketListener())
                    .join();
        } catch (Exception e) {
            System.err.println("[MCPClientWebSocket] Connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public CompletableFuture<String> sendMessage(String payload) {
        String taskId = extractTaskId(payload);
        CompletableFuture<String> future = new CompletableFuture<>();
        taskFutures.put(taskId, future);

        if (webSocket != null) {
            webSocket.sendText(payload, true)
                    .exceptionally(ex -> {
                        future.completeExceptionally(ex);
                        return null;
                    });
        } else {
            future.completeExceptionally(new IllegalStateException("Not connected to MCP"));
        }

        return future;
    }

    private String extractTaskId(String payload) {
        try {
            Map<String, Object> map = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
            return map.getOrDefault("taskId", "").toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract taskId from payload", e);
        }
    }

    private class WebSocketListener implements Listener {

        @Override
        public void onOpen(WebSocket ws) {
            System.out.println("[MCPClientWebSocket] Connected to MCP server");
            Listener.super.onOpen(ws);
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            String msg = data.toString();
            try {
                // Try parsing as JSON
                Map<String, Object> messageMap = objectMapper.readValue(msg, new TypeReference<Map<String, Object>>() {});
                String taskId = messageMap.getOrDefault("taskId", "").toString();

                CompletableFuture<String> future = taskFutures.remove(taskId);
                if (future != null) {
                    future.complete(msg);
                } else {
                    System.out.println("[MCPClientWebSocket] Received message for unknown taskId: " + taskId);
                }
            } catch (Exception e) {
                // Handle non-JSON (plain text) messages safely
                System.out.println("[MCPClientWebSocket] Non-JSON message received: " + msg);

                // Optionally, try to complete any future if you can extract taskId manually
                taskFutures.values().forEach(future -> future.complete(msg));
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
