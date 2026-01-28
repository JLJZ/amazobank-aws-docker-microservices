package com.amazobank.crm.clientservice.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.amazobank.crm.clientservice.api.dto.UpdateClientRequest;
import com.amazobank.crm.clientservice.domain.Client;
import com.amazobank.crm.clientservice.domain.ClientStatus;
import com.amazobank.crm.clientservice.domain.VerificationStatus;
import com.amazobank.crm.clientservice.repo.ClientRepository;

@Service
public class ClientService {
    private final ClientRepository repo;

    public ClientService(ClientRepository repo, SqsService sqsService) {
        this.repo = repo;
    }

    public List<Client> findAll() {
        return repo.findAll();
    }

    public List<Client> findByAgentId(UUID agentId) {
        return repo.findByAgentId(agentId);
    }

    public Optional<Client> findById(@NonNull UUID id) {
        return repo.findById(id);
    }

    public Optional<Client> findByEmail(String email) {
        return repo.findByEmail(email);
    }

    public Optional<Client> findByPhoneNumber(String phoneNumber) {
        return repo.findByPhoneNumber(phoneNumber);
    }

    public Client update(Client client) {
        return save(client);
    }

    public void verify(Client client) {
        /**
         * Simulate real world verification has completed.
         */
        client.setVerificationStatus(VerificationStatus.Verified);
        repo.save(client);
    }

    public Client save(@NonNull Client client) {
        return repo.save(client);
    }

    public void softDelete(@NonNull Client client) {
        // Soft delete: set client status to "Deleted"
        client.setClientStatus(ClientStatus.Deleted);
        repo.save(client);
    }
}
