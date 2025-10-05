package com.agenticai.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletionStage;


import com.fasterxml.jackson.databind.ObjectMapper;

public class MCPClientWebSocket {

    private final WebSocket webSocket;
    private final Map<String, CompletableFuture<String>> pendingTasks = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MCPClientWebSocket(String serverUrl) throws Exception {
        System.out.println("[MCPClientWebSocket] Connecting to " + serverUrl);
        HttpClient client = HttpClient.newHttpClient();
        CompletableFuture<WebSocket> wsFuture = client.newWebSocketBuilder()
                .buildAsync(URI.create(serverUrl + "/mcp"), new MCPWebSocketListener());
        this.webSocket = wsFuture.join();
        System.out.println("[MCPClientWebSocket] ✅ Connected successfully to MCP server");
    }

    public CompletableFuture<String> sendMessage(String taskId, String message) {
        try {
            String payload = String.format("{\"taskId\":\"%s\",\"task\":\"%s\"}", taskId, message);
            System.out.println("[MCPClientWebSocket] Sending message: " + payload);
            webSocket.sendText(payload, true);
            return CompletableFuture.completedFuture("Message sent successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Sends a task with taskId and JSON payload asynchronously.
     */
    public CompletableFuture<String> sendTask(String taskId, String payload) {
        try {
            // Build JSON message that MCP expects
            Map<String, Object> message = Map.of(
                    "taskId", taskId,
                    "task", payload
            );

            String jsonMessage = objectMapper.writeValueAsString(message);
            System.out.println("[MCPClientWebSocket] Sending task to MCP: " + jsonMessage);

            CompletableFuture<String> future = new CompletableFuture<>();
            pendingTasks.put(taskId, future);
            webSocket.sendText(jsonMessage, true);

            return future;
        } catch (Exception e) {
            CompletableFuture<String> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        }
    }

    /**
     * Internal WebSocket Listener class for handling responses.
     */
    private class MCPWebSocketListener implements Listener {

        @Override
        public void onOpen(WebSocket webSocket) {
            System.out.println("[MCPClientWebSocket] Connection opened with MCP server");
            Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            String message = data.toString();
            System.out.println("[MCPClientWebSocket] 🔔 Received message from MCP: " + message);

            try {
                Map<String, Object> responseMap = objectMapper.readValue(message, Map.class);
                String taskId = (String) responseMap.get("taskId");

                if (taskId != null && pendingTasks.containsKey(taskId)) {
                    CompletableFuture<String> future = pendingTasks.remove(taskId);
                    if (future != null) {
                        future.complete(message);
                    }
                }
            } catch (Exception e) {
                System.err.println("[MCPClientWebSocket] Failed to parse MCP response: " + e.getMessage());
            }

            return Listener.super.onText(webSocket, data, last);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            System.err.println("[MCPClientWebSocket] ❌ WebSocket error: " + error.getMessage());
        }
    }
}
