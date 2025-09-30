package com.agenticai.memory;

import java.util.ArrayList;
import java.util.List;

public class MemoryStore {
    private final List<String> taskLog = new ArrayList<>();

    public void logTask(String taskDescription) {
        taskLog.add("TASK: " + taskDescription);
    }

    public void storeResponse(String response) {
        taskLog.add("RESPONSE: " + response);
    }

    public List<String> getTaskLog() {
        return taskLog;
    }
}
