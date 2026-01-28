package com.amazobank.crm.accountservice.repository;

import com.amazobank.crm.accountservice.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, String> {
    List<Transaction> findByAccountAccountId(String accountId);

    Optional<Transaction> findByAccountAccountIdAndTransactionId(String accountId, String transactionId);
}
