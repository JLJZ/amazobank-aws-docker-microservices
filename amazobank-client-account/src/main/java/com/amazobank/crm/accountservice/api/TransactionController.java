package com.amazobank.crm.accountservice.api;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.amazobank.crm.accountservice.api.dto.TransactionDto;
import com.amazobank.crm.accountservice.domain.Account;
import com.amazobank.crm.accountservice.domain.Transaction;
import com.amazobank.crm.accountservice.service.AccountService;
import com.amazobank.crm.accountservice.service.TransactionMapper;
import com.amazobank.crm.accountservice.service.TransactionService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/accounts/{accountId}/transactions")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private final AccountService accountService;
    private final TransactionService transactionService;

    public TransactionController(AccountService accountService, TransactionService transactionService) {
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<List<TransactionDto>> getTransactions(@PathVariable @NonNull String accountId, Authentication authentication) {
        log.info("Fetching transactions for account: {}", accountId);

        Optional<Account> accOpt = accountService.findById(accountId);
        if(accOpt.isEmpty()) {
            log.warn("Account not found: {}", accountId);
            return ResponseEntity.notFound().build();
        }

        String agentId = authentication.getName();
        Account account = accOpt.get();
        if(!account.getAgentId().equals(agentId)) {
            log.warn("Forbidden access attempt: agent {} tried to access transactions for account {} owned by agent {}", 
                     agentId, accountId, account.getAgentId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<Transaction> transactions = transactionService.findByAccountId(accountId);
        log.warn("Forbidden access attempt: agent {} tried to access transactions for account {} owned by agent {}", 
                     agentId, accountId, account.getAgentId());
        return ResponseEntity.ok(TransactionMapper.toDto(transactions));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionDto> getTransaction(
        @PathVariable @NonNull String accountId, 
        @PathVariable @NonNull String transactionId,
        Authentication authentication
        ) {
        
        log.info("Fetching transaction: {} for account: {}", transactionId, accountId);
        
        Optional<Account> accOpt = accountService.findById(accountId);
        if (accOpt.isEmpty()) {
            // The account id does not exist
            log.warn("Account not found: {}", accountId);
            return ResponseEntity.notFound().build();
        }

        String agentId = authentication.getName();
        Account account = accOpt.get();
        if(!account.getAgentId().equals(agentId)) {
            // The agent does not manage the account
            log.warn("Forbidden access attempt: agent {} tried to access transaction {} for account {} owned by agent {}", 
                     agentId, transactionId, accountId, account.getAgentId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<Transaction> result = transactionService.findByAccountIdAndTransactionId(accountId, transactionId);

        if(result.isEmpty()) {
            // The transaction id does not exist
            log.warn("Transaction not found: {} for account: {}", transactionId, accountId);
            return ResponseEntity.notFound().build();
        }
        Transaction transaction = result.get();
        log.debug("Successfully retrieved transaction: {} for account: {}", transactionId, accountId);
        
        return ResponseEntity.ok(TransactionMapper.toDto(transaction));
    }
}
