
package com.email.test.controller;

import com.email.test.dto.EmailTestRequest;
import com.email.test.service.EmailService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/api/email")
public class EmailTestController {

    private final EmailService emailService;
    private final String testToken;

    public EmailTestController(
            EmailService emailService,
            @Value("${email.test.token}") String testToken
    ) {
        this.emailService = emailService;
        this.testToken = testToken;
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> sendTestEmail(
            @RequestHeader(
                    value = "X-Email-Test-Token",
                    required = false
            ) String suppliedToken,

            @RequestBody(required = false)
            EmailTestRequest request
    ) {

        // 1. Verify access token
        if (suppliedToken == null ||
                !MessageDigest.isEqual(
                        testToken.getBytes(StandardCharsets.UTF_8),
                        suppliedToken.getBytes(StandardCharsets.UTF_8)
                )) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of(
                            "success", false,
                            "emailAccepted", false,
                            "message", "Unauthorized email test request."
                    )
            );
        }

        // 2. Validate request
        if (request == null) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "Request body is missing."
                    )
            );
        }

        String recipient = request.recipient() == null
                ? "" : request.recipient().trim();

        String subject = request.subject() == null
                ? "" : request.subject().trim();

        String message = request.message() == null
                ? "" : request.message().trim();

        // 3. Validate required fields
        if (recipient.isEmpty()
                || subject.isEmpty()
                || message.isEmpty()) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "All fields are required."
                    )
            );
        }

        // 4. Validate recipient
        if (!recipient.matches(
                "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
        )) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "Enter a valid email address."
                    )
            );
        }

        // 5. Validate length
        if (subject.length() > 200 ||
                message.length() > 5000) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "Subject or message exceeds allowed length."
                    )
            );
        }

        // 6. Send using Brevo
        try {

            System.out.println("========== BREVO EMAIL TEST ==========");
            System.out.println("Recipient: " + recipient);

            String messageId = emailService.sendTestEmail(
                    recipient,
                    subject,
                    message
            );

            System.out.println("Brevo accepted the email.");
            System.out.println("Message ID: " + messageId);
            System.out.println("======================================");

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                    Map.of(
                            "success", true,
                            "emailAccepted", true,
                            "provider", "Brevo",
                            "messageId", messageId,
                            "message",
                            "Brevo accepted your email request. Check your inbox and spam folder."
                    )
            );

        } catch (Exception e) {

            System.err.println("BREVO EMAIL ERROR:");
            e.printStackTrace();

            return ResponseEntity.status(
                    HttpStatus.BAD_GATEWAY
            ).body(
                    Map.of(
                            "success", false,
                            "emailAccepted", false,
                            "provider", "Brevo",
                            "message",
                            "Email service failed. Check backend logs."
                    )
            );
        }
    }
}
