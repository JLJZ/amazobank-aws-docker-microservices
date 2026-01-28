package com.amazobank.crm.accountservice.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.net.InetAddress;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> healthCheck() throws Exception {
        return Map.of(
                "service", "account-service",
                "status", "healthy",
                "host", InetAddress.getLocalHost().getHostAddress()
        );
    }
}
