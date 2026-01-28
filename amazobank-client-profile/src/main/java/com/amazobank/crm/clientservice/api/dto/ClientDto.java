package com.amazobank.crm.clientservice.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.amazobank.crm.clientservice.domain.ClientStatus;
import com.amazobank.crm.clientservice.domain.VerificationStatus;

public record ClientDto(
        UUID clientId,
        UUID agentId,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String gender,
        String email,
        String phoneNumber,
        String address,
        String city,
        String state,
        String country,
        String postalCode,
        VerificationStatus verificationStatus,
        ClientStatus clientStatus
) {}
