package com.amazobank.crm.accountservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {
    @Id
    @Column(length = 36)
    private String accountId;

    @Column(nullable = false, length = 36)
    private String clientId;

    @Column(nullable = false, length = 36)
    private String agentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus accountStatus;

    @Column(nullable = false)
    private LocalDate openingDate;

    @Column(nullable = false)
    private Double initialDeposit;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(length = 20)
    private String branchId;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = false)
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();
}
