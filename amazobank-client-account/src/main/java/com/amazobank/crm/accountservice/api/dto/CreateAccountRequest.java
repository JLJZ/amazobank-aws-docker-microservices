package com.amazobank.crm.accountservice.api.dto;

import com.amazobank.crm.accountservice.domain.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateAccountRequest(
    @NotBlank String clientId,
    @NotBlank String clientEmail,
    @NotNull AccountType accountType,
    @NotNull Double initialDeposit,
    @NotBlank String currency,
    String branchId,
    @NotNull LocalDate openingDate
) {}
