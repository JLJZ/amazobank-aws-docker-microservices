package com.amazobank.crm.accountservice.service;

import java.util.List;

import com.amazobank.crm.accountservice.api.dto.TransactionDto;
import com.amazobank.crm.accountservice.domain.Transaction;

public class TransactionMapper {

    public static TransactionDto toDto(Transaction transaction) {
        return new TransactionDto(
            transaction.getTransactionId(),
            transaction.getClientId(),
            transaction.getAccount().getAccountId(),
            transaction.getTransactionType(),
            transaction.getAmount(),
            transaction.getDate(),
            transaction.getStatus()
        );
    }

    public static List<TransactionDto> toDto(List<Transaction> transactions) {
        return transactions.stream()
            .map(TransactionMapper::toDto)
            .toList();
    }
}
