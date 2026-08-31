package com.example.tier2.payment;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long id) {
        Optional<Payment> payment = paymentService.getPayment(id);

        if (payment.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        PaymentResponse response = new PaymentResponse(
                payment.get().getPaymentId(),
                payment.get().getStatus()
        );
        return ResponseEntity.ok(response);
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