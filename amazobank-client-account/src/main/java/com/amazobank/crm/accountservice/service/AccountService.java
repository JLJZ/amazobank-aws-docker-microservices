package com.amazobank.crm.accountservice.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.amazobank.crm.accountservice.domain.Account;
import com.amazobank.crm.accountservice.domain.AccountStatus;
import com.amazobank.crm.accountservice.repository.AccountRepository;

@Service
public class AccountService {
    private final AccountRepository repo;

    public AccountService(AccountRepository repo) {
        this.repo = repo;
    }

    public List<Account> findAll() {
        return repo.findAll();
    }

    public Optional<Account> findById(String id) {
        return repo.findById(id);
    }

    public List<Account> findByClientId(String clientId) {
        return repo.findByClientId(clientId);
    }

    public List<Account> findByAgentId(String agentId) {
        return repo.findByAgentId(agentId);
    }

    public Account save(Account account) {
        return repo.save(account);
    }

    public void softDelete(Account account) {
        account.setAccountStatus(AccountStatus.Deleted);
        repo.save(account);
    }
}
