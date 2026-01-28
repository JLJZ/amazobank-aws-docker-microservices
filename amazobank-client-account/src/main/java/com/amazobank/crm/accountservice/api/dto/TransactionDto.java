package com.amazobank.crm.accountservice.api.dto;

import java.time.LocalDate;

import com.amazobank.crm.accountservice.domain.TransactionStatus;
import com.amazobank.crm.accountservice.domain.TransactionType;

public record TransactionDto(
    String transactionId,
    String clientId,
    String accountId,
    TransactionType transactionType,
    Double amount,
    LocalDate date,
    TransactionStatus status
) {}
