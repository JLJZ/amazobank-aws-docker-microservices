package com.amazobank.crm.accountservice.service;

import com.amazobank.crm.accountservice.domain.Account;
import com.amazobank.crm.accountservice.api.dto.AccountDto;

public class AccountMapper {
    public static AccountDto toDto(Account a) {
        return new AccountDto(
            a.getAccountId(),
            a.getClientId(),
            a.getAgentId(),
            a.getAccountType(),
            a.getAccountStatus(),
            a.getOpeningDate(),
            a.getInitialDeposit(),
            a.getCurrency(),
            a.getBranchId()
        );
    }
}
