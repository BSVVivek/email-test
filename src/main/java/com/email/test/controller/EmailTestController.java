
package com.email.test.controller;

import com.email.test.dto.EmailTestRequest;
import com.email.test.service.EmailService;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/email")
public class EmailTestController {

    private final EmailService emailService;

    public EmailTestController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> sendTestEmail(
            @RequestBody(required = false) EmailTestRequest request) {

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

        if (!recipient.matches(
                "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", "Invalid email address."
                    )
            );
        }

        try {

            System.out.println("========== BREVO EMAIL TEST ==========");
            System.out.println("Recipient: " + recipient);

            String messageId = emailService.sendTestEmail(
                    recipient,
                    subject,
                    message
            );

            System.out.println("Brevo accepted email.");
            System.out.println("Message ID: " + messageId);
            System.out.println("======================================");

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                    Map.of(
                            "success", true,
                            "emailAccepted", true,
                            "provider", "Brevo",
                            "messageId", messageId,
                            "message",
                            "Brevo accepted your email request. Check your inbox."
                    )
            );

        } catch (Exception e) {

            System.err.println("BREVO EMAIL FAILED:");
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
