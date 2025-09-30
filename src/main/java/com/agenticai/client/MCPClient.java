package com.agenticai.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MCPClient {

    private final String serverUrl;
    private final HttpClient httpClient;

    public MCPClient(String serverUrl) {
        this.serverUrl = serverUrl;
        this.httpClient = HttpClient.newHttpClient();
    }

    public String sendPrompt(String prompt) {
        try {
            String json = "{"+prompt+": "+ prompt.replace("", "\"") + "}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + "/mcp"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error communicating with MCP Server: " + e.getMessage();
        }
    }
}