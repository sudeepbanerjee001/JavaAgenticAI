package com.agenticai.service;

import org.springframework.stereotype.Service;
import com.agenticai.client.MCPClientWebSocket;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class AgentService {

    private final MCPClientWebSocket mcpClient;
    private static final int CHUNK_SIZE = 3000; // Approx chars per chunk

    public AgentService(MCPClientWebSocket mcpClient) {
        this.mcpClient = mcpClient;
    }

    // ----------------- Process normal tasks -----------------
    public String process(String task, String taskId) {
        String intent = detectIntent(task);
        String prompt = buildPrompt(task, intent);

        try {
            CompletableFuture<String> futureResponse = mcpClient.sendMessage(taskId, prompt);
            return futureResponse.get();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error communicating with MCP/LLM: " + e.getMessage();
        }
    }

    // ----------------- Process repository for microservice migration -----------------
    public String process(String repoPath, String taskId, String customPrompt) {
        try {
            List<String> javaChunks = readJavaFilesInChunks(repoPath);
            StringBuilder finalResponse = new StringBuilder();

            // Stage 0: Role Definition
            String rolePrompt = stage0RoleDefinitionPrompt();
            if (customPrompt != null && !customPrompt.isEmpty()) {
                rolePrompt += "\n\n--- USER CUSTOM PROMPT ---\n" + customPrompt;
            }
            finalResponse.append(sendToMCP(taskId, rolePrompt)).append("\n\n");

            // Stage 1: Repository Analysis (chunked)
            for (String chunk : javaChunks) {
                String stage1Prompt = stage1RepoAnalysisPrompt(chunk);
                if (customPrompt != null && !customPrompt.isEmpty()) {
                    stage1Prompt += "\n\n--- USER CUSTOM PROMPT ---\n" + customPrompt;
                }
                finalResponse.append(sendToMCP(taskId, stage1Prompt)).append("\n\n");
            }

            // Stage 2: Microservice Planning
            String stage2Prompt = stage2MicroservicePlanningPrompt();
            if (customPrompt != null && !customPrompt.isEmpty()) {
                stage2Prompt += "\n\n--- USER CUSTOM PROMPT ---\n" + customPrompt;
            }
            finalResponse.append(sendToMCP(taskId, stage2Prompt)).append("\n\n");

            // Stage 3: Refactoring & Code Conversion
            String stage3Prompt = stage3RefactoringPrompt();
            if (customPrompt != null && !customPrompt.isEmpty()) {
                stage3Prompt += "\n\n--- USER CUSTOM PROMPT ---\n" + customPrompt;
            }
            finalResponse.append(sendToMCP(taskId, stage3Prompt)).append("\n\n");

            // Stage 4: Review & Optimization
            String stage4Prompt = stage4ReviewPrompt();
            if (customPrompt != null && !customPrompt.isEmpty()) {
                stage4Prompt += "\n\n--- USER CUSTOM PROMPT ---\n" + customPrompt;
            }
            finalResponse.append(sendToMCP(taskId, stage4Prompt)).append("\n\n");

            return finalResponse.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "Error processing repository: " + e.getMessage();
        }
    }

    // ----------------- Detect intent -----------------
    private String detectIntent(String task) {
        String lower = task.toLowerCase();
        if (lower.contains("refactor") || lower.contains("microservice") || lower.contains("architecture"))
            return "microservice";
        if (lower.contains("java") || lower.contains("python") || lower.contains("c++"))
            return "generate";
        return "general";
    }

    // ----------------- Build prompt -----------------
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

    // ----------------- Stage-wise prompts -----------------
    private String buildMicroservicePrompt() {
        StringBuilder sb = new StringBuilder();

        sb.append("You are an expert senior Java developer AI with deep knowledge of:\n")
                .append("- Java 8+ and Spring Boot\n")
                .append("- Microservices architecture and best practices\n")
                .append("- Design patterns (Singleton, Factory, Observer, CQRS, Saga, etc.)\n")
                .append("- Refactoring legacy codebases to modern architectures\n")
                .append("- Clean code, SOLID principles, testing, logging, and error handling\n\n");

        sb.append("Your task: Refactor a legacy Java application into a Spring Boot microservices architecture while maintaining all functionalities and applying best practices.\n\n");

        sb.append("Stage 1: Repository Analysis\n")
                .append("Instructions:\n")
                .append("1. Summarize packages, modules, classes.\n")
                .append("2. Identify dependencies, frameworks.\n")
                .append("3. Highlight code smells, technical debt.\n")
                .append("4. Suggest potential independent microservices.\n\n");

        sb.append("Stage 2: Microservice Architecture Planning\n")
                .append("Instructions:\n")
                .append("1. Define microservices, responsibilities, interactions.\n")
                .append("2. Suggest API endpoints and design patterns.\n")
                .append("3. Suggest database separation, caching, communication patterns.\n\n");

        sb.append("Stage 3: Refactoring & Code Conversion\n")
                .append("Instructions:\n")
                .append("1. Convert legacy classes/modules to microservices.\n")
                .append("2. Maintain functionality.\n")
                .append("3. Apply clean code, SOLID principles, exception handling, unit tests.\n\n");

        sb.append("Stage 4: Iterative Review & Optimization\n")
                .append("Instructions:\n")
                .append("1. Check redundancy, coupling, missing functionality.\n")
                .append("2. Suggest improvements and optimizations.\n")
                .append("3. Add logging, monitoring, and error handling best practices.\n\n");

        return sb.toString();
    }

    private String stage0RoleDefinitionPrompt() {
        return "You are an expert senior Java developer AI with deep knowledge of Java 8+, Spring Boot, microservices, design patterns, clean code, SOLID principles, testing, logging, and error handling. "
                + "Your task: Refactor a legacy Java application into a microservices architecture while maintaining all functionalities.";
    }

    private String stage1RepoAnalysisPrompt(String javaCodeChunk) {
        return "Stage 1: Repository Analysis\n"
                + "Instructions:\n"
                + "1. Summarize packages, modules, classes.\n"
                + "2. Identify dependencies, frameworks.\n"
                + "3. Highlight code smells or technical debt.\n"
                + "Analyze this code chunk:\n"
                + javaCodeChunk;
    }

    private String stage2MicroservicePlanningPrompt() {
        return "Stage 2: Microservice Architecture Planning\n"
                + "Instructions:\n"
                + "1. Define microservices, responsibilities, interactions.\n"
                + "2. Suggest API endpoints, design patterns.\n"
                + "3. Suggest database separation, caching, communication patterns.\n\n"
                + "Plan the microservices architecture based on analyzed code.";
    }

    private String stage3RefactoringPrompt() {
        return "Stage 3: Refactoring & Code Conversion\n"
                + "Instructions:\n"
                + "1. Convert legacy classes/modules to microservices.\n"
                + "2. Maintain functionality.\n"
                + "3. Apply SOLID, clean code, exception handling, unit tests.\n"
                + "Generate microservice code.";
    }

    private String stage4ReviewPrompt() {
        return "Stage 4: Iterative Review & Optimization\n"
                + "Instructions:\n"
                + "1. Check redundancy, coupling, missing functionality.\n"
                + "2. Suggest improvements and optimizations.\n"
                + "3. Add logging, monitoring, and error handling best practices.\n"
                + "Review and optimize generated microservices code.";
    }

    // ----------------- Helper methods -----------------
    private List<String> readJavaFilesInChunks(String repoPath) throws Exception {
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

    private String sendToMCP(String taskId, String prompt) throws Exception {
        CompletableFuture<String> future = mcpClient.sendMessage(taskId, prompt);
        return future.get();
    }
}
