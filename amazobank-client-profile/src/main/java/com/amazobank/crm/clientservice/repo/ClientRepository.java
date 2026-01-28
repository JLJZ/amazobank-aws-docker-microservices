package com.amazobank.crm.clientservice.repo;

import com.amazobank.crm.clientservice.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {
    List<Client> findByAgentId(UUID agentId);
    Optional<Client> findByEmail(String email);
    Optional<Client> findByPhoneNumber(String phoneNumber);
}
