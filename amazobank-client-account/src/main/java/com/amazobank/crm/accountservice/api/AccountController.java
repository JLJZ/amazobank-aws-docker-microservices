package com.amazobank.crm.accountservice.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.amazobank.crm.accountservice.api.dto.AccountDto;
import com.amazobank.crm.accountservice.api.dto.CreateAccountRequest;
import com.amazobank.crm.accountservice.api.dto.UpdateAccountRequest;
import com.amazobank.crm.accountservice.domain.Account;
import com.amazobank.crm.accountservice.domain.AccountStatus;
import com.amazobank.crm.accountservice.service.AccountMapper;
import com.amazobank.crm.accountservice.service.AccountService;
import com.amazobank.crm.accountservice.service.SqsService;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private static final Logger log = LoggerFactory.getLogger(AccountController.class);

    @Autowired
    private AccountService service;

    @Autowired SqsService sqsService;
    /**
     * Retrieve all accounts managed by the agent
     */
    @GetMapping
    public ResponseEntity<List<AccountDto>> getAll(@RequestParam(required = false) String clientId, Authentication authentication) {
        String agentId = authentication.getName();
                
        log.info("Fetching all accounts for agent: {}", agentId);
        List<Account> accounts = service.findByAgentId(agentId);
        log.debug("Found {} accounts for agent: {}", accounts.size(), agentId);

        return ResponseEntity.ok(
            accounts.stream()
                    .map(AccountMapper::toDto)
                    .toList()
        );
    }

    /**
     * Retrieve one account by ID (restricted to managing agent).
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable String id, Authentication authentication) {
        String agentId = authentication.getName();

        log.info("Fetching account with id: {}", id);
        
        Optional<Account> result = service.findById(id);
        if(result.isEmpty()) {
            log.warn("Account not found: {}", id);
            return ResponseEntity.notFound().build();
        }

        Account acc = result.get();
        if(!agentId.equals(acc.getAgentId())) {
            log.warn("Forbidden access attempt: agent {} tried to access account {} owned by agent {}", 
                     agentId, id, acc.getAgentId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                    "error", "Forbidden",
                    "message", String.format("AccountID : {} is not managed by AgentID: {}", id, agentId).toString()
                ));
        }

        log.debug("Successfully retrieved account: {}", id);
        return ResponseEntity.ok(AccountMapper.toDto(acc));
    }

    /**
     * Create a new account (agent ID inferred from JWT).
     */
    @PostMapping
    public ResponseEntity<AccountDto> create(@RequestBody CreateAccountRequest req, Authentication authentication) {
        String agentId = authentication.getName();

        log.info("Creating new account for agent: {}, clientId: {}, accountType: {}", 
                 agentId, req.clientId(), req.accountType());

        Account acc = Account.builder()
                .accountId(UUID.randomUUID().toString())
                .clientId(req.clientId())
                .agentId(agentId)
                .accountType(req.accountType())
                .accountStatus(AccountStatus.Active)
                .openingDate(LocalDate.now())
                .initialDeposit(req.initialDeposit())
                .currency(req.currency())
                .branchId(req.branchId())
                .build();

        Account saved = service.save(acc);
        log.info("Account created successfully: accountId={}, clientId={}, agentId={}", 
                 saved.getAccountId(), saved.getClientId(), saved);
        sqsService.sendEmailNotification(req.clientEmail(), "Your account was created successfully on " + LocalDateTime.now());
        return ResponseEntity.status(201)
                .body(AccountMapper.toDto(saved));
    }

    /**
     * Update an existing account (only by owner agent or admin).
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
        @PathVariable String id, 
        @RequestBody UpdateAccountRequest req,
        Authentication authentication
    ) {
        String agentId = authentication.getName();

        log.info("Updating account: {}", id);

        Optional<Account> accOpt = service.findById(id);
        if (accOpt.isEmpty()) {
            log.warn("Update failed: account not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
            "error", "Not Found",
            "message", "AccountID: " + id + " does not exist."
            ));
        }

        Account acc = accOpt.get();
        if (!acc.getAgentId().equals(agentId)) {
            log.warn("Update forbidden: agent {} tried to update account {} owned by agent {}", 
                     agentId, id, acc.getAgentId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                    "error", "Forbidden",
                    "message", String.format("AccountID : {} is not managed by AgentID: {}", id, agentId).toString()
                )
            );
        }

        if (req.accountType() != null) acc.setAccountType(req.accountType());
        if (req.accountStatus() != null) acc.setAccountStatus(req.accountStatus());
        if (req.initialDeposit() != null) acc.setInitialDeposit(req.initialDeposit());
        if (req.currency() != null) acc.setCurrency(req.currency());
        if (req.branchId() != null) acc.setBranchId(req.branchId());
        if (req.openingDate() != null) acc.setOpeningDate(req.openingDate());

        log.info("Account updated successfully: {}", id);
        return ResponseEntity.ok(AccountMapper.toDto(service.save(acc)));
    }

    /**
     * Delete account (only if account is owned by agent).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id, Authentication authentication) {
        String agentId = authentication.getName();

        log.info("Deleting account: {}", id);

        Optional<Account> accOpt = service.findById(id);
        if (accOpt.isEmpty()) {
            log.warn("Delete failed: account not found: {}", id);
            return ResponseEntity.notFound().build();
        }

        Account acc = accOpt.get();
        if (!acc.getAgentId().equals(agentId)) {
            log.warn("Delete forbidden: agent {} tried to delete account {} owned by agent {}", 
                     agentId, id, acc.getAgentId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                    "error", "Forbidden",
                    "message", String.format("AccountID : {} is not managed by AgentID: {}", id, agentId).toString()
                )
            );
        }
        service.softDelete(acc);
        log.info("Account deleted successfully: {}", id);
        return ResponseEntity.noContent().build();
    }
}
