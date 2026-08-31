package com.example.tier1.payment;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public PaymentResponse createPayment(@RequestBody PaymentRequest request) {
        Payment payment = paymentService.createPayment(
                request.idempotencyKey(),
                request.fromAccount(),
                request.toAccount(),
                request.amount()
        );
        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getStatus()
        );
    }

    public record PaymentRequest(
            String idempotencyKey,
            Long fromAccount,
            Long toAccount,
            Long amount
    ) {}

    public record PaymentResponse(
            Long paymentId,
            String status
    ){}

}