package com.agenticai.controller;

import com.agenticai.service.AgentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/agent")
public class AgentRestController {

    private final AgentService agentService;

    public AgentRestController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/task")
    public String submitTask(@RequestBody String taskDescription) {
        return agentService.handleTask(taskDescription);
    }
}
