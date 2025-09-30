package com.agenticai.processor;

import com.agenticai.memory.MemoryStore;

public class TaskProcessor {

    private final MemoryStore memoryStore;

    public TaskProcessor(MemoryStore memoryStore) {
        this.memoryStore = memoryStore;
    }

    public String process(String modelResponse) {
        memoryStore.storeResponse(modelResponse);
        return modelResponse;
    }
}
