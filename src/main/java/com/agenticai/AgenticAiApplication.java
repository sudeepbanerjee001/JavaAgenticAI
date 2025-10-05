package com.agenticai;

import com.agenticai.client.MCPClientWebSocket;
import com.agenticai.memory.MemoryStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AgenticAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgenticAiApplication.class, args);
    }

    @Bean
    public MCPClientWebSocket mcpClientWebSocket() {
        try {
            return new MCPClientWebSocket("ws://localhost:8080/mcp");
        } catch (Exception e) {
            // Wrap in RuntimeException so Spring can handle it
            throw new RuntimeException("Failed to initialize MCPClientWebSocket", e);
        }
    }

    @Bean
    public MemoryStore memoryStore() {
        return new MemoryStore();
    }
}
