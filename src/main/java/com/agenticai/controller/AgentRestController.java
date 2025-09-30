package com.agenticai.controller;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

import com.agenticai.service.AgentService;

@RestController
@RequestMapping("/agent")
public class AgentRestController {

    private final AgentService agentService;

    public AgentRestController(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * Handle a user task request and return the AI's answer.
     *
     * Request JSON:
     * {
     *   "taskId": "task-123",
     *   "task": "Explain this Java code..."
     * }
     *
     * Response JSON:
     * {
     *   "taskId": "task-123",
     *   "answer": "AI explanation..."
     * }
     */
    @PostMapping("/task")
    public Map<String, String> handleTask(@RequestBody Map<String, String> body) {
        String taskId = body.get("taskId");
        String task = body.get("task");

        if (taskId == null || task == null) {
            throw new IllegalArgumentException("Both 'taskId' and 'task' are required in request body");
        }

        // Pass both task and taskId to AgentService
        String answer = agentService.process(task, taskId);

        return Map.of(
                "taskId", taskId,
                "answer", answer
        );
    }

    /**
     * Handle a repository request for microservice migration.
     *
     * Request JSON:
     * {
     *   "taskId": "task-456",
     *   "repoPath": "/path/to/local/repo"
     *   // or a Git URL like "https://github.com/user/repo.git"
     * }
     *
     * Response JSON:
     * {
     *   "taskId": "task-456",
     *   "migrationPlan": "AI generated microservice plan and code..."
     * }
     */
    @PostMapping("/repo")
    public Map<String, String> handleRepo(@RequestBody Map<String, String> body) {
        String taskId = body.get("taskId");
        String repoPath = body.get("repoPath");

        if (taskId == null || repoPath == null) {
            throw new IllegalArgumentException("Both 'taskId' and 'repoPath' are required in request body");
        }

        // Call the new AgentService method to process the repository
        String migrationPlan = agentService.process(repoPath, taskId);

        return Map.of(
                "taskId", taskId,
                "migrationPlan", migrationPlan
        );
    }
}
