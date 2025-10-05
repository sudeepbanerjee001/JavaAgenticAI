package com.agenticai.controller;

import com.agenticai.service.AgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/agent")
public class AgentRestController {

    private final AgentService agentService;

    @Autowired
    public AgentRestController(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * Endpoint to send a user message and get a processed response.
     * Example: POST /agent/message
     * Body: { "message": "Refactor my Java service" }
     */
    @PostMapping("/message")
    public String handleUserMessage(@RequestBody UserMessageRequest request) {
        String userMessage = request.getMessage();

        // Detect intent
        String intent = agentService.detectIntent(userMessage);

        // Process the message using AgentService
        String response = agentService.processTask(userMessage);

        // Optionally, append intent info to response
        return String.format("Intent detected: %s\nResponse:\n%s", intent, response);
    }

    // -------------------- Request DTO --------------------
    public static class UserMessageRequest {
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
