package com.amazobank.crm.accountservice.repository;

import com.amazobank.crm.accountservice.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AccountRepository extends JpaRepository<Account, String> {
    List<Account> findByClientId(String clientId);
    List<Account> findByAgentId(String agentId);
}
