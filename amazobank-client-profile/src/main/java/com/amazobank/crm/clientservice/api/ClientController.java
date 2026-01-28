package com.amazobank.crm.clientservice.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.amazobank.crm.clientservice.api.dto.CreateClientRequest;
import com.amazobank.crm.clientservice.api.dto.UpdateClientRequest;
import com.amazobank.crm.clientservice.domain.Client;
import com.amazobank.crm.clientservice.domain.ClientStatus;
import com.amazobank.crm.clientservice.service.ClientMapper;
import com.amazobank.crm.clientservice.service.ClientService;
import com.amazobank.crm.clientservice.service.SqsService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private static final Logger log = LoggerFactory.getLogger(ClientController.class);

    @Autowired
    private ClientService service;

    @Autowired
    private SqsService sqsService;

    // ---------------- GET ALL CLIENTS ----------------
    @GetMapping
    public ResponseEntity<List<Client>> getAll(HttpServletRequest request, Authentication authentication) {
        UUID agentId = UUID.fromString(authentication.getName());

        log.info("Fetching all clients for agent: {}", agentId);
        List<Client> clients = service.findByAgentId(agentId);
        log.debug("Found {} clients for agent: {}", clients.size(), agentId);

        return ResponseEntity.ok(
            clients
        );
    }

    // ---------------- GET ONE CLIENT ----------------
    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable UUID id, HttpServletRequest request, Authentication authentication) {
        log.info("Fetching client with id: {}", id);

        Optional<Client> clientOpt = service.findById(id);
        if (clientOpt.isEmpty()) {
            log.warn("Client not found: {}", id);
            return ResponseEntity.status(404).body(Map.of("message", "Client not found"));
        }

        Client client = clientOpt.get();

        // Authorization check
        UUID agentId = UUID.fromString(authentication.getName());
        if (!client.getAgentId().equals(agentId)) {
            log.warn("Forbidden access attempt: agent {} tried to access client {} owned by agent {}", 
                     agentId, id, client.getAgentId());
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden"));
        }

        log.debug("Agent: {} accessed client: {}", agentId, id);
        return ResponseEntity.ok(ClientMapper.toDto(client));
    }

    // ---------------- CREATE CLIENT ----------------
    @PostMapping
    public ResponseEntity<?> createClient(@Valid @RequestBody CreateClientRequest req, HttpServletRequest request, Authentication authentication) {
        UUID agentId = UUID.fromString(authentication.getName());

        // Duplicate checks
        if (service.findByEmail(req.email()).isPresent()) {
            log.warn("Client creation failed: email already exists: {}", req.email());
            return ResponseEntity.badRequest().body(Map.of("message", "Email already exists."));
        }
        if (service.findByPhoneNumber(req.phoneNumber()).isPresent()) {
            log.warn("Client creation failed: phone number already exists: {}", req.phoneNumber());
            return ResponseEntity.badRequest().body(Map.of("message", "Phone number already exists."));
        }

        Client client = ClientMapper.toEntity(req, agentId);
        service.save(client);

        log.info("Client created successfully: clientId={}, agentId={}", client.getClientId(), agentId);
        sqsService.sendEmailNotification(client.getEmail(), "Your profile was created successfully on " + LocalDateTime.now());
        return ResponseEntity.status(201).body(ClientMapper.toDto(client));
    }

    // ---------------- VERIFY CLIENT ----------------
    @PostMapping("{clientId}/verify")
    public ResponseEntity<?> verify(@NonNull @PathVariable UUID clientId, HttpServletRequest request, Authentication authentication) {
        Optional<Client> existingOpt = service.findById(clientId);
        if (existingOpt.isEmpty()) {
            log.warn("Client not found: {}", clientId);
            return ResponseEntity.status(404).body(Map.of("message", "Client not found"));
        }
        Client client = existingOpt.get();

        // Authorization check
        UUID agentId = UUID.fromString(authentication.getName());
        if (!client.getAgentId().equals(agentId)) {
        log.warn("Forbidden access attempt: agent {} tried to access client {} owned by agent {}", 
                    agentId, clientId, client.getAgentId());
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden"));
        }
        
        service.verify(client);
        log.info("Client verified successfully: clientId={}, agentId={}", client.getClientId(), client.getAgentId());
        sqsService.sendEmailNotification(client.getEmail(), "Your profile was verified successfully on " + LocalDateTime.now());
        return ResponseEntity.ok().build();
    }

    // ---------------- UPDATE CLIENT ----------------
    @PutMapping("/{id}")
    public ResponseEntity<?> updateClient(@PathVariable UUID id, @RequestBody UpdateClientRequest req, HttpServletRequest request, Authentication authentication) {
        log.info("Updating client: {}", id);
        
        Optional<Client> existingOpt = service.findById(id);
        if (existingOpt.isEmpty()) {
            log.warn("Update failed: client not found: {}", id);
            return ResponseEntity.status(404).body(Map.of("message", "Client not found"));
        }

        Client existing = existingOpt.get();

        // Check if client is soft deleted
        if (existing.getClientStatus() == ClientStatus.Deleted) {
            log.warn("Update failed: attempt to update soft-deleted client: {}", id);
            return ResponseEntity.status(410).body(Map.of("message", "Client has been deleted and cannot be updated"));
        }


        // Authorization check
        UUID agentId = UUID.fromString(authentication.getName());
        if (!existing.getAgentId().equals(agentId)) {
            log.warn("Update forbidden: agent {} tried to update client {} owned by agent {}", 
                     agentId, id, existing.getAgentId());
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden"));
        }

        // Duplicate checks
        if (req.email() != null && !req.email().equalsIgnoreCase(existing.getEmail()) &&
                service.findByEmail(req.email()).isPresent()) {
            log.warn("Update failed: email already exists: {}", req.email());
            return ResponseEntity.badRequest().body(Map.of("message", "Email already exists."));
        }
        if (req.phoneNumber() != null && !req.phoneNumber().equals(existing.getPhoneNumber()) &&
                service.findByPhoneNumber(req.phoneNumber()).isPresent()) {
            log.warn("Update failed: phone number already exists: {}", req.phoneNumber());
            return ResponseEntity.badRequest().body(Map.of("message", "Phone number already exists."));
        }

        ClientMapper.updateEntity(existing, req);
        Client updated = service.update(existing);
        log.info("Client updated successfully: {}", updated.getClientId());
        sqsService.sendEmailNotification(updated.getEmail(), "Your profile was updated successfully on " + LocalDateTime.now());
        return ResponseEntity.ok(ClientMapper.toDto(updated));
    }

    // ---------------- DELETE CLIENT ----------------
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteClient(@PathVariable @NonNull UUID id, HttpServletRequest request, Authentication authentication) {
        log.info("Deleting client: {}", id);
        
        Optional<Client> clientOpt = service.findById(id);
        if (clientOpt.isEmpty()) {
            log.warn("Delete failed: client not found: {}", id);
            return ResponseEntity.status(404).body(Map.of("message", "Client not found"));
        }

        Client client = clientOpt.get();

        UUID agentId = UUID.fromString(authentication.getName());
        if (!client.getAgentId().equals(agentId)) {
            log.warn("Delete forbidden: agent {} tried to delete client {} owned by agent {}", 
                     agentId, id, client.getAgentId());
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden"));
        }

        service.softDelete(client);
        log.info("Agent: {} deleted client: {} successfully", id);
        sqsService.sendEmailNotification(client.getEmail(), "Your profile was deleted on " + LocalDateTime.now());
        return ResponseEntity.ok(Map.of("message", "Client deleted successfully"));
    }
}
