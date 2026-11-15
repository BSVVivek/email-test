
package com.email.test.service;

import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private final JsonMapper jsonMapper =
            JsonMapper.builder().build();

    private final HttpClient httpClient;

    private final String apiUrl;
    private final String apiKey;
    private final String senderEmail;
    private final String senderName;

    public EmailService(
            @Value("${brevo.api-url}") String apiUrl,
            @Value("${brevo.api-key}") String apiKey,
            @Value("${brevo.sender-email}") String senderEmail,
            @Value("${brevo.sender-name}") String senderName
    ) {

        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.senderEmail = senderEmail;
        this.senderName = senderName;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String sendTestEmail(
            String recipient,
            String subject,
            String message
    ) throws IOException, InterruptedException {

        // 1. Sender details
        Map<String, Object> sender = new HashMap<>();

        sender.put("name", senderName);
        sender.put("email", senderEmail);

        // 2. Recipient details
        Map<String, Object> recipientDetails = new HashMap<>();

        recipientDetails.put("email", recipient);

        // 3. Build Brevo payload
        Map<String, Object> payload = new HashMap<>();

        payload.put("sender", sender);
        payload.put("to", List.of(recipientDetails));
        payload.put("subject", subject);
        payload.put("textContent", message);

        // 4. Convert payload into JSON
        String json = jsonMapper.writeValueAsString(payload);

        // 5. Prepare HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(20))
                .header("accept", "application/json")
                .header("api-key", apiKey)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        System.out.println("Connecting to Brevo API...");

        // 6. Send email request
        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        System.out.println(
                "Brevo HTTP Status: " + response.statusCode()
        );

        // 7. Check response
        // Check Brevo response
        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            throw new IllegalStateException(
                    "Brevo API error: " + response.body()
            );
        }

// Extract message ID
        Map<?, ?> result = jsonMapper.readValue(
                response.body(),
                Map.class
        );

        Object rawMessageId = result.get("messageId");

        String messageId = rawMessageId == null
                ? "Unknown"
                : rawMessageId.toString();

        System.out.println("Email accepted by Brevo.");
        System.out.println("Message ID: " + messageId);

        return messageId;
    }
}
