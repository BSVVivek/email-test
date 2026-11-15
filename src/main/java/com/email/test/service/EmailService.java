
package com.email.test.service;

import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
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

        this.apiUrl = apiUrl == null ? "" : apiUrl.trim();
        this.apiKey = apiKey;
        this.senderEmail = senderEmail;
        this.senderName = senderName;

        if (this.apiUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "Brevo API URL is missing."
            );
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Brevo API key is missing."
            );
        }

        if (senderEmail == null || senderEmail.isBlank()) {
            throw new IllegalArgumentException(
                    "Brevo sender email is missing."
            );
        }

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    public String sendTestEmail(
            String recipient,
            String subject,
            String message
    ) throws IOException, InterruptedException {

        System.out.println();
        System.out.println("========== BREVO EMAIL TEST ==========");
        System.out.println("Recipient: " + recipient);

        // 1. Validate the API endpoint
        URI endpoint;

        try {
            endpoint = URI.create(apiUrl);
        } catch (IllegalArgumentException e) {
            throw new IOException(
                    "Invalid Brevo API URL. Check brevo.api-url.",
                    e
            );
        }

        if (!"https".equalsIgnoreCase(endpoint.getScheme())
                || !"api.brevo.com".equalsIgnoreCase(endpoint.getHost())
                || !"/v3/smtp/email".equals(endpoint.getPath())) {

            throw new IOException(
                    "Incorrect Brevo API endpoint: " + endpoint
            );
        }

        String host = endpoint.getHost();

        // 2. Check DNS resolution before contacting Brevo
        System.out.println("Checking DNS for: " + host);

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);

            if (addresses.length == 0) {
                throw new UnknownHostException(
                        "No IP addresses found for " + host
                );
            }

            System.out.println("DNS resolution successful.");

        } catch (UnknownHostException e) {

            System.err.println(
                    "DNS ERROR: Cannot resolve " + host
            );

            throw new IOException(
                    "Cannot resolve Brevo server. "
                            + "Check your internet connection and DNS settings.",
                    e
            );
        }

        // 3. Build sender details
        Map<String, Object> sender = new HashMap<>();

        sender.put("name", senderName);
        sender.put("email", senderEmail);

        // 4. Build recipient details
        Map<String, Object> recipientDetails = new HashMap<>();

        recipientDetails.put("email", recipient);

        // 5. Build Brevo email payload
        Map<String, Object> payload = new HashMap<>();

        payload.put("sender", sender);
        payload.put("to", List.of(recipientDetails));
        payload.put("subject", subject);
        payload.put("textContent", message);

        // 6. Convert payload to JSON
        String json = jsonMapper.writeValueAsString(payload);

        // 7. Build HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(endpoint)
                .timeout(Duration.ofSeconds(45))
                .header("accept", "application/json")
                .header("api-key", apiKey)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        System.out.println("Connecting to Brevo API...");

        // 8. Send request
        HttpResponse<String> response;

        try {

            response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

        } catch (HttpTimeoutException e) {

            System.err.println(
                    "BREVO TIMEOUT: No response within 45 seconds."
            );

            throw new IOException(
                    "Brevo request timed out. "
                            + "The email may already have been accepted. "
                            + "Check Brevo logs before retrying.",
                    e
            );

        } catch (ConnectException e) {

            System.err.println(
                    "BREVO CONNECTION ERROR: "
                            + "Could not establish an HTTPS connection."
            );

            throw new IOException(
                    "Could not connect to Brevo. "
                            + "Check DNS, internet access, firewall or VPN settings.",
                    e
            );

        } catch (IOException e) {

            System.err.println(
                    "BREVO NETWORK ERROR: " + e.getMessage()
            );

            throw e;
        }

        // 9. Print Brevo response
        System.out.println(
                "Brevo HTTP Status: " + response.statusCode()
        );

        // 10. Handle unsuccessful responses
        if (response.statusCode() < 200
                || response.statusCode() >= 300) {

            System.err.println(
                    "Brevo rejected the request: " + response.body()
            );

            throw new IOException(
                    "Brevo API returned HTTP "
                            + response.statusCode()
                            + ": "
                            + response.body()
            );
        }

        // 11. Extract Brevo message ID
        Map<?, ?> result = jsonMapper.readValue(
                response.body(),
                Map.class
        );

        Object rawMessageId = result.get("messageId");

        String messageId = rawMessageId == null
                ? "Unknown"
                : rawMessageId.toString();

        // 12. Success
        System.out.println("Email accepted by Brevo.");
        System.out.println("Message ID: " + messageId);
        System.out.println("======================================");
        System.out.println();

        return messageId;
    }
}
