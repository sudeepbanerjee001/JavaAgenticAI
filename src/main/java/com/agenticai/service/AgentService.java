package com.agenticai.service;

import com.agenticai.client.MCPClientWebSocket;
import com.agenticai.memory.MemoryStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);
    private final MCPClientWebSocket mcpClient;
    private final MemoryStore memoryStore;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final int CHUNK_SIZE = 3000;

    public AgentService(MCPClientWebSocket mcpClient, MemoryStore memoryStore) {
        this.mcpClient = mcpClient;
        this.memoryStore = memoryStore;
    }

    // ----------------- Unified processTask -----------------
    public String processTask(String task) {
        String taskId = "task-" + System.currentTimeMillis();
        try {
            // Create JSON payload with taskId and task
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("taskId", taskId);
            payloadMap.put("task", task);
            String payload = objectMapper.writeValueAsString(payloadMap);

            // Send payload using updated MCPClientWebSocket
            CompletableFuture<String> future = mcpClient.sendMessage(payload);

            String response = future.get();
            log.info("[AgentService] processTask response: {}", response);

            memoryStore.storeResponse(response);
            return response;

        } catch (Exception e) {
            log.error("[AgentService] Error processing task", e);
            return "Error: " + e.getMessage();
        }
    }

    // ----------------- Intent detection -----------------
    public String detectIntent(String task) {
        String lower = task.toLowerCase();
        if (lower.contains("refactor") || lower.contains("microservice") || lower.contains("architecture"))
            return "microservice";
        if (lower.contains("java") || lower.contains("python") || lower.contains("c++"))
            return "generate";
        return "general";
    }
}
