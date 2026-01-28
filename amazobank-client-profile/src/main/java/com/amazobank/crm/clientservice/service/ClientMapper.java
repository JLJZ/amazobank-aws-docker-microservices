package com.amazobank.crm.clientservice.service;

import com.amazobank.crm.clientservice.api.dto.*;
import com.amazobank.crm.clientservice.domain.Client;
import com.amazobank.crm.clientservice.domain.ClientStatus;
import com.amazobank.crm.clientservice.domain.Gender;
import com.amazobank.crm.clientservice.domain.VerificationStatus;

import java.util.UUID;

public class ClientMapper {

    public static ClientDto toDto(Client c) {
        return new ClientDto(
            c.getClientId(),
            c.getAgentId(),
            c.getFirstName(),
            c.getLastName(),
            c.getDateOfBirth(),
            c.getGender().name(),
            c.getEmail(),
            c.getPhoneNumber(),
            c.getAddress(),
            c.getCity(),
            c.getState(),
            c.getCountry(),
            c.getPostalCode(),
            c.getVerificationStatus(),
            c.getClientStatus()
        );
    }

    public static Client toEntity(CreateClientRequest req, UUID agentId) {
        Client c = new Client();
        c.setAgentId(agentId);
        c.setFirstName(req.firstName());
        c.setLastName(req.lastName());
        c.setDateOfBirth(req.dateOfBirth());
        c.setGender(Gender.valueOf(req.gender()));
        c.setEmail(req.email());
        c.setPhoneNumber(req.phoneNumber());
        c.setAddress(req.address());
        c.setCity(req.city());
        c.setState(req.state());
        c.setCountry(req.country());
        c.setPostalCode(req.postalCode());
        c.setVerificationStatus(VerificationStatus.Unverified);
        c.setClientStatus(ClientStatus.Active);
        return c;
    }

    public static void updateEntity(Client c, UpdateClientRequest req) {
        if (req.firstName() != null) c.setFirstName(req.firstName());
        if (req.lastName() != null) c.setLastName(req.lastName());
        if (req.dateOfBirth() != null) c.setDateOfBirth(req.dateOfBirth());
        if (req.gender() != null) c.setGender(Gender.valueOf(req.gender()));
        if (req.email() != null) c.setEmail(req.email());
        if (req.phoneNumber() != null) c.setPhoneNumber(req.phoneNumber());
        if (req.address() != null) c.setAddress(req.address());
        if (req.city() != null) c.setCity(req.city());
        if (req.state() != null) c.setState(req.state());
        if (req.country() != null) c.setCountry(req.country());
        if (req.postalCode() != null) c.setPostalCode(req.postalCode());
        if (req.clientStatus() != null) c.setClientStatus(req.clientStatus());
    }
}
