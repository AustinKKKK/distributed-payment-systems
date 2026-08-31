package com.example.tier2.payment;

public record PaymentRequestedEvent(
        Long paymentId,
        Long fromAccount,
        Long toAccount,
        Long amount
) {}