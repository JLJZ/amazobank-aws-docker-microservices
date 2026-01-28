package com.amazobank.crm.clientservice.domain;

import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "ClientID")
    private UUID clientId;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "AgentID", nullable = false)
    private UUID agentId;

    @Column(name = "FirstName", nullable = false, length = 50)
    private String firstName;

    @Column(name = "LastName", nullable = false, length = 50)
    private String lastName;

    @Column(name = "DateOfBirth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "Gender", nullable = false, length = 20)
    private Gender gender;

    @Column(name = "Email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "PhoneNumber", nullable = false, unique = true, length = 20)
    private String phoneNumber;

    @Column(name = "Address", nullable = false, length = 100)
    private String address;

    @Column(name = "City", nullable = false, length = 50)
    private String city;

    @Column(name = "State", nullable = false, length = 50)
    private String state;

    @Column(name = "Country", nullable = false, length = 50)
    private String country;

    @Column(name = "PostalCode", nullable = false, length = 10)
    private String postalCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "VerificationStatus", nullable = false, length = 10)
    private VerificationStatus verificationStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "ClientStatus", nullable = false, length = 10)
    private ClientStatus clientStatus;
}
