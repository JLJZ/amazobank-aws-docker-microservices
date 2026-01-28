package com.amazobank.crm.userservice.api;

import java.net.InetAddress;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
final class HealthController {
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() throws Exception {
        return ResponseEntity.ok(Map.of(
            "message", "Service is healthy",
            "service", "user-service",
            "ip", InetAddress.getLocalHost().getHostAddress()
        ));
    }
}
