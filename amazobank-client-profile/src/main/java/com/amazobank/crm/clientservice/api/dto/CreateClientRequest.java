package com.amazobank.crm.clientservice.api.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record CreateClientRequest(
        @NotBlank @Size(min=2,max=50) String firstName,
        @NotBlank @Size(min=2,max=50) String lastName,
        @NotNull LocalDate dateOfBirth,
        @NotBlank String gender,
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp="^\\+\\d{10,15}$") String phoneNumber,
        @NotBlank String address,
        @NotBlank String city,
        @NotBlank String state,
        @NotBlank String country,
        @NotBlank String postalCode
) {}
