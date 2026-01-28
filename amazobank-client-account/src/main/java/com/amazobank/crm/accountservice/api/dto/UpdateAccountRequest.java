package com.amazobank.crm.accountservice.api.dto;

import com.amazobank.crm.accountservice.domain.AccountStatus;
import com.amazobank.crm.accountservice.domain.AccountType;

import java.time.LocalDate;

public record UpdateAccountRequest(
    AccountType accountType,
    AccountStatus accountStatus,
    LocalDate openingDate,
    Double initialDeposit,
    String currency,
    String branchId
) {}
