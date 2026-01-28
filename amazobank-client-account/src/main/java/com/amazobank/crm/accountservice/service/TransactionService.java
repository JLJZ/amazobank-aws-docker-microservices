package com.amazobank.crm.accountservice.service;

import com.amazobank.crm.accountservice.domain.Transaction;
import com.amazobank.crm.accountservice.repository.AccountRepository;
import com.amazobank.crm.accountservice.repository.TransactionRepository;

import lombok.NonNull;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransactionService(TransactionRepository transactionRepository, AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    public List<Transaction> findByAccountId(@NonNull String accountId) {
        return transactionRepository.findByAccountAccountId(accountId);
    }

    public Optional<Transaction> findByAccountIdAndTransactionId(@NonNull String accountId, String transactionId) {
        if (!accountRepository.existsById(accountId)) {
            return Optional.empty();
        }
        return transactionRepository.findByAccountAccountIdAndTransactionId(accountId, transactionId);
    }
}
