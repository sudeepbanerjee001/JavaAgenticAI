package com.agenticai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/agent")
public class AgentRestController {

    @PostMapping("/task")
    public ResponseEntity<Map<String, Object>> handleTask(@RequestBody Map<String, Object> payload) {
        try {
            // Extract fields safely with null checks
            String userMessage = payload.get("userMessage") != null ? payload.get("userMessage").toString() : "";
            String action = payload.get("action") != null ? payload.get("action").toString() : "unknown";
            String sessionId = payload.get("sessionId") != null ? payload.get("sessionId").toString() : "";
            String intent = payload.get("intent") != null ? payload.get("intent").toString() : "";

            // Log the received payload
            System.out.println("[AGENT LOG] Received task: " + userMessage + " | action: " + action + " | intent: " + intent);

            // Here, implement your actual AI logic or code generation
            // Example: For demonstration, just echoing back a simple Java palindrome code if codeAssist
            String response;
            if ("codeAssist".equalsIgnoreCase(intent)) {
                response = """
                        // Java program to check palindrome
                        import java.util.Scanner;

                        public class Palindrome {
                            public static void main(String[] args) {
                                Scanner sc = new Scanner(System.in);
                                System.out.print("Enter a string: ");
                                String str = sc.nextLine();
                                String reversed = new StringBuilder(str).reverse().toString();
                                if (str.equals(reversed)) {
                                    System.out.println("Palindrome");
                                } else {
                                    System.out.println("Not a palindrome");
                                }
                            }
                        }
                        """;
            } else {
                response = "Action '" + action + "' received. Intent: " + intent;
            }

            // Build response map
            Map<String, Object> toolResponse = Map.of(
                    "sessionId", sessionId,
                    "response", response
            );

            return ResponseEntity.ok(toolResponse);

        } catch (Exception e) {
            e.printStackTrace();
            // Return safe error response
            Map<String, Object> errorResponse = Map.of(
                    "error", "Error processing task: " + e.getMessage()
            );
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
