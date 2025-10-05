package com.agenticai.controller;

import com.agenticai.service.AgentService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/invoke")
public class AgentRestController {

    private final AgentService agentService;

    public AgentRestController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping
    public String invoke(@RequestBody Map<String, Object> request) {
        String userMessage = (String) request.get("userMessage");
        String taskId = (String) request.get("taskId");

        System.out.println("[Agentic AI] Received request: \"" + userMessage + "\" (taskId=" + taskId + ")");

        // Detect intent
        String intent = agentService.detectIntent(userMessage);
        System.out.println("[Agentic AI] Detected intent: " + intent);

        // Process message
        String response = agentService.process(userMessage, taskId);

        System.out.println("[Agentic AI] Response for taskId=" + taskId + ": " + response);

        return response;
    }
}
