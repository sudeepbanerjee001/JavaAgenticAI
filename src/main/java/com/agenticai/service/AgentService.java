package com.agenticai.service;

import com.agenticai.client.MCPClientWebSocket;
import com.agenticai.memory.MemoryStore;
import com.agenticai.processor.TaskProcessor;
import org.springframework.stereotype.Service;

@Service
public class AgentService {

    private final MCPClientWebSocket mcpClient;
    private final TaskProcessor taskProcessor;
    private final MemoryStore memoryStore;

    public AgentService(MCPClientWebSocket mcpClient, MemoryStore memoryStore) {
        this.mcpClient = mcpClient;
        this.memoryStore = memoryStore;
        this.taskProcessor = new TaskProcessor(memoryStore);
    }

    public String handleTask(String taskDescription) {
        memoryStore.logTask(taskDescription);
        String response = mcpClient.sendMessage(taskDescription);
        return taskProcessor.process(response);
    }
}
