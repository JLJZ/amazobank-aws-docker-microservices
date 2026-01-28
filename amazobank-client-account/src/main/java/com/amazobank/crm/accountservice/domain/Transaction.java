package com.amazobank.crm.accountservice.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "Transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @Column(name = "TransactionID", length = 36, nullable = false)
    private String transactionId;

    @Column(name = "ClientID", length = 36, nullable = false)
    private String clientId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "AccountID", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "TransactionType", length = 1, nullable = false)
    private TransactionType transactionType;

    @Column(name = "Amount", nullable = false)
    private Double amount;

    @Column(name = "Date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false)
    private TransactionStatus status;
}
