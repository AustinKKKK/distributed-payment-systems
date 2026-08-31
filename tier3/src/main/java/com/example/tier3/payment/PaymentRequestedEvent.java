package com.example.tier3.payment;

public record PaymentRequestedEvent(
        Long paymentId,
        Long fromAccount,
        Long toAccount,
        Long amount
) {}