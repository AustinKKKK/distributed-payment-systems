package com.example.tier2.payment;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentConsumer {

    private final PaymentService paymentService;

    public PaymentConsumer(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaListener(
            topics = "payment.requested",
            groupId = "payment-processor",
            concurrency = "3"
    )
    public void handlePaymentRequested(PaymentRequestedEvent event) {
        Payment payment = paymentService.getPayment(event.paymentId())
                .orElseThrow(() -> new IllegalStateException(
                    "Payment not found: " + event.paymentId()));

        paymentService.processLedgerAndFinish(
                payment,
                event.fromAccount(),
                event.toAccount(),
                event.amount()
        );
    }

}
