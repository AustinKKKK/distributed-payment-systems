package com.example.tier4.payment;

public record PaymentRequestedEvent(
        Long paymentId,
        Long fromAccount,
        Long toAccount,
        Long amount
) {}