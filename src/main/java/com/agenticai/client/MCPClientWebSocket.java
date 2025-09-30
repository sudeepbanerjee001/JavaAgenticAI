package com.agenticai.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

public class MCPClientWebSocket {

    private WebSocket webSocket;
    private final CountDownLatch latch = new CountDownLatch(1);
    private String latestResponse;

    public MCPClientWebSocket(String serverUrl) {
        HttpClient client = HttpClient.newHttpClient();
        this.webSocket = client.newWebSocketBuilder()
                .buildAsync(URI.create(serverUrl + "/mcp"), new Listener() {

                    @Override
                    public void onOpen(WebSocket ws) {
                        System.out.println("Connected to MCP WebSocket");
                        Listener.super.onOpen(ws);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
                        latestResponse = data.toString();
                        latch.countDown();
                        return Listener.super.onText(ws, data, last);
                    }

                    @Override
                    public void onError(WebSocket ws, Throwable error) {
                        error.printStackTrace();
                        Listener.super.onError(ws, error);
                    }

                }).join();
    }

    public String sendMessage(String message) {
        // Corrected type
        CompletableFuture<WebSocket> sendFuture = webSocket.sendText(message, true);
        try {
            sendFuture.get();   // wait for sending to complete
            latch.await();      // wait for response
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return latestResponse;
    }
}
