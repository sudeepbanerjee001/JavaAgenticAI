package com.agenticai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class MCPClientWebSocket {

    private final WebSocket webSocket;

    // TaskId → Future mapping (existing functionality)
    private final Map<String, CompletableFuture<String>> pendingTasks = new ConcurrentHashMap<>();

    // Session tracking: taskId → associated session (for repo/multi-chunk context)
    private final Map<String, String> taskSessions = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Connects to MCP WebSocket server
     *
     * @param serverUrl e.g. "ws://localhost:8081"
     */
    public MCPClientWebSocket(String serverUrl) {
        HttpClient client = HttpClient.newHttpClient();
        this.webSocket = client.newWebSocketBuilder()
                .buildAsync(URI.create(serverUrl + "/mcp"), new Listener() {

                    @Override
                    public void onOpen(WebSocket ws) {
                        System.out.println("✅ Connected to MCP WebSocket");
                        Listener.super.onOpen(ws);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                        String rawResponse = data.toString();
                        try {
                            JsonNode jsonNode = objectMapper.readTree(rawResponse);
                            String taskId = jsonNode.has("taskId") ? jsonNode.get("taskId").asText() : null;
                            String response = jsonNode.has("response") ? jsonNode.get("response").asText() : rawResponse;

                            if (taskId != null) {
                                CompletableFuture<String> future = pendingTasks.remove(taskId);
                                if (future != null) {
                                    future.complete(response);
                                }
                                // Optional: clean up session mapping after completion
                                taskSessions.remove(taskId);
                            } else {
                                completeFirstPendingTask(response);
                            }

                        } catch (Exception e) {
                            completeFirstPendingTask(rawResponse);
                        }
                        return Listener.super.onText(ws, data, last);
                    }

                    @Override
                    public void onError(WebSocket ws, Throwable error) {
                        error.printStackTrace();
                        Listener.super.onError(ws, error);
                    }
                }).join();
    }

    /**
     * Send a message to the MCP/LLM server
     *
     * @param taskId     unique identifier for this request
     * @param message    the task/prompt to send
     * @param sessionId  optional session ID to track repo/multi-chunk tasks
     * @return CompletableFuture that completes when response arrives
     */
    public CompletableFuture<String> sendMessage(String taskId, String message, String sessionId) {
        if (sessionId != null) {
            taskSessions.put(taskId, sessionId);
        }

        CompletableFuture<String> future = new CompletableFuture<>();
        pendingTasks.put(taskId, future);

        String payload = "{\"taskId\":\"" + taskId + "\",\"task\":\"" + message + "\"}";
        webSocket.sendText(payload, true)
                .exceptionally(ex -> {
                    future.completeExceptionally(ex);
                    pendingTasks.remove(taskId);
                    taskSessions.remove(taskId);
                    return null;
                });

        return future;
    }

    // Overloaded method for backward compatibility (existing code)
    public CompletableFuture<String> sendMessage(String taskId, String message) {
        return sendMessage(taskId, message, null);
    }

    /**
     * Complete the first pending task with a given response (used for plain text responses)
     */
    private void completeFirstPendingTask(String response) {
        if (!pendingTasks.isEmpty()) {
            String firstTaskId = pendingTasks.keySet().iterator().next();
            CompletableFuture<String> future = pendingTasks.remove(firstTaskId);
            if (future != null) {
                future.complete(response);
            }
            taskSessions.remove(firstTaskId);
        } else {
            System.err.println("Received response but no pending tasks: " + response);
        }
    }

    // Optional: get sessionId for a given taskId (for AgentService tracking)
    public String getSessionForTask(String taskId) {
        return taskSessions.get(taskId);
    }
}
