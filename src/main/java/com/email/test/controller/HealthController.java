package com.email.test.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {

        Map<String, Object> response = Map.of(

                "success", true,

                "application", "LakshmiEmailTest",

                "message",
                "Lakshmi Email Test Backend is running!",

                "timestamp", Instant.now().toString()

        );

        return ResponseEntity.ok(response);
    }
}