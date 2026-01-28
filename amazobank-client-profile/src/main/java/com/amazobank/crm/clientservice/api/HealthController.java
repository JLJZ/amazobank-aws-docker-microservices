package com.amazobank.crm.clientservice.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.net.InetAddress;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() throws Exception {
        return ResponseEntity.ok(Map.of(
            "service", "client-profile-service",
            "status", "healthy",
            "host", InetAddress.getLocalHost().getHostAddress()
        ));
    }
}
