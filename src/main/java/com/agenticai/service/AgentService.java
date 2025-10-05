package com.agenticai.service;

import org.springframework.stereotype.Service;
import com.agenticai.client.MCPClientWebSocket;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);
    private final MCPClientWebSocket mcpClient;
    private static final int CHUNK_SIZE = 3000; // chars per chunk

    public AgentService(MCPClientWebSocket mcpClient) {
        this.mcpClient = mcpClient;
    }

    // ----------------- Existing normal task processing -----------------
    public String process(String task, String taskId) {
        String intent = detectIntent(task);
        log.info("[AgentService] Detected intent: {}", intent);

        String prompt = buildPrompt(task, intent);
        log.info("[AgentService] Built prompt:\n{}", prompt);

        try {
            CompletableFuture<String> futureResponse = mcpClient.sendMessage(taskId, prompt);
            String response = futureResponse.get();
            log.info("[AgentService] Received response from MCP:\n{}", response);
            return response;
        } catch (Exception e) {
            log.error("[AgentService] Error communicating with MCP/LLM", e);
            return "Error communicating with MCP/LLM: " + e.getMessage();
        }
    }

    // ----------------- Context-aware multi-chunk repo analyzer -----------------
    public String analyzeRepo(String repoPath, String taskId) {
        try {
            List<String> javaChunks = readJavaFilesInChunks(repoPath);
            StringBuilder finalResponse = new StringBuilder();

            // Stage 0: Role definition (context-aware)
            String rolePrompt = "You are a context-aware Java repository analyst AI. " +
                    "You will receive multiple chunks of Java code. " +
                    "Remember all previous chunks and integrate information cumulatively. " +
                    "Do not hallucinate any code or package names. Only report what exists.";
            finalResponse.append(sendToMCP(taskId, rolePrompt)).append("\n\n");

            int chunkCounter = 1;
            for (String chunk : javaChunks) {
                String chunkPrompt = "Chunk " + chunkCounter + " of " + javaChunks.size() + " from repository: " + repoPath + "\n\n"
                        + "Analyze this chunk and remember it for cumulative summary:\n"
                        + chunk;
                finalResponse.append(sendToMCP(taskId, chunkPrompt)).append("\n\n");
                chunkCounter++;
            }

            // Stage final: Generate cumulative summary
            String finalPrompt = "Using all previous chunks, provide a complete, structured, high-level summary of the repository at '"
                    + repoPath + "'. Include folders, packages, modules, classes, interfaces, enums, and their responsibilities. " +
                    "Also describe core functionality. Only include what exists in the code.";
            finalResponse.append(sendToMCP(taskId, finalPrompt)).append("\n\n");

            return finalResponse.toString();

        } catch (Exception e) {
            log.error("[AgentService] Error analyzing repository", e);
            return "Error analyzing repository: " + e.getMessage();
        }
    }

    // ----------------- Helper methods -----------------
    private List<String> readJavaFilesInChunks(String repoPath) throws IOException {
        List<String> chunks = new ArrayList<>();
        File repo = new File(repoPath);
        List<File> javaFiles = listJavaFiles(repo);

        StringBuilder buffer = new StringBuilder();
        for (File file : javaFiles) {
            String content = new String(Files.readAllBytes(file.toPath()));
            if (buffer.length() + content.length() > CHUNK_SIZE) {
                chunks.add(buffer.toString());
                buffer.setLength(0);
            }
            buffer.append(content).append("\n");
        }
        if (buffer.length() > 0) {
            chunks.add(buffer.toString());
        }
        return chunks;
    }

    private List<File> listJavaFiles(File folder) {
        List<File> javaFiles = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files == null) return javaFiles;

        for (File f : files) {
            if (f.isDirectory()) {
                javaFiles.addAll(listJavaFiles(f));
            } else if (f.getName().endsWith(".java")) {
                javaFiles.add(f);
            }
        }
        return javaFiles;
    }

    private String sendToMCP(String taskId, String prompt) throws ExecutionException, InterruptedException {
        CompletableFuture<String> future = mcpClient.sendMessage(taskId, prompt);
        String resp = future.get();
        log.info("[AgentService] sendToMCP response:\n{}", resp);
        return resp;
    }

    // ----------------- Existing intent detection -----------------
    public String detectIntent(String task) {
        String lower = task.toLowerCase();
        if (lower.contains("refactor") || lower.contains("microservice") || lower.contains("architecture"))
            return "microservice";
        if (lower.contains("java") || lower.contains("python") || lower.contains("c++"))
            return "generate";
        return "general";
    }

    // ----------------- Existing prompt builder -----------------
    private String buildPrompt(String task, String intent) {
        StringBuilder promptBuilder = new StringBuilder();

        switch (intent) {
            case "microservice":
                promptBuilder.append(buildMicroservicePrompt());
                promptBuilder.append("\n\n--- USER CUSTOM PROMPT ---\n");
                promptBuilder.append(task);
                break;
            case "generate":
                promptBuilder.append("You are a highly skilled Java Programmer:\n");
                promptBuilder.append(task);
                break;
            default:
                promptBuilder.append("Provide an explanation or answer for:\n");
                promptBuilder.append(task);
                break;
        }
        return promptBuilder.toString();
    }

    private String buildMicroservicePrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert senior Java developer AI with deep knowledge of:\n")
                .append("- Java 8+ and Spring Boot\n")
                .append("- Microservices architecture and best practices\n")
                .append("- Design patterns (Singleton, Factory, Observer, CQRS, Saga, etc.)\n")
                .append("- Refactoring legacy codebases to modern architectures\n")
                .append("- Clean code, SOLID principles, testing, logging, and error handling\n\n");

        sb.append("Your task: Refactor a legacy Java application into a Spring Boot microservices architecture while maintaining all functionalities and applying best practices.\n\n");
        return sb.toString();
    }
}
