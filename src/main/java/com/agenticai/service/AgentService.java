package com.agenticai.service;

import com.agenticai.client.MCPClientWebSocket;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AgentService {

    private final MCPClientWebSocket mcpClientWebSocket;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Map to track taskId -> CompletableFuture
    private final ConcurrentHashMap<String, CompletableFuture<String>> taskFutures = new ConcurrentHashMap<>();

    public AgentService(@Lazy MCPClientWebSocket mcpClientWebSocket) {
        this.mcpClientWebSocket = mcpClientWebSocket;
    }

    public CompletableFuture<String> submitTask(String task, String intent) {
        try {
            String taskId = "task-" + UUID.randomUUID();
            CompletableFuture<String> future = new CompletableFuture<>();
            taskFutures.put(taskId, future);

            // Prepare JSON payload
            String payload = objectMapper.writeValueAsString(new TaskPayload(taskId, task, intent));

            // Send to MCP
            mcpClientWebSocket.sendMessage(payload);

            return future;
        } catch (Exception e) {
            CompletableFuture<String> failed = new CompletableFuture<>();
            failed.completeExceptionally(e);
            return failed;
        }
    }

    /**
     * Called by MCPClientWebSocket when a message is received.
     */
    public void handleMCPResponse(String response) {
        try {
            TaskPayload payload = objectMapper.readValue(response, TaskPayload.class);
            String taskId = payload.taskId;

            CompletableFuture<String> future = taskFutures.remove(taskId);
            if (future != null) {
                future.complete(response);
            } else {
                System.out.println("[AgentService] Received response for unknown taskId: " + taskId);
            }
        } catch (Exception e) {
            System.err.println("[AgentService] Failed to handle MCP response: " + e.getMessage());
        }
    }

    // Helper class for payload
    private static class TaskPayload {
        public String taskId;
        public String task;
        public String intent;

        public TaskPayload() {}

        public TaskPayload(String taskId, String task, String intent) {
            this.taskId = taskId;
            this.task = task;
            this.intent = intent;
        }
    }
}
