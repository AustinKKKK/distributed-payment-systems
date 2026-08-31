package com.example.tier2.payment;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long entryId;

    @Column(nullable = false)
    private Long paymentId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected LedgerEntry() {}

    public LedgerEntry(Long paymentId, Long accountId, Long amount) {
        this.paymentId = paymentId;
        this.accountId = accountId;
        this.amount = amount;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getEntryId() {
        return entryId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getAmount() {
        return amount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}

