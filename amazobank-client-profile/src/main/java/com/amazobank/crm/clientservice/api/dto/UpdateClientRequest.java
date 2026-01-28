package com.amazobank.crm.clientservice.api.dto;

import java.time.LocalDate;

import com.amazobank.crm.clientservice.domain.ClientStatus;

public record UpdateClientRequest(
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
        ClientStatus clientStatus
) {}
