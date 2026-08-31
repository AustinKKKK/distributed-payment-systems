package com.example.tier2.payment;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
public class PaymentService {

    private static final String TOPIC = "payment.requested";

    private final PaymentRepository paymentRepository;
    private final LedgerRepository ledgerRepository;
    private final KafkaTemplate<String, PaymentRequestedEvent> kafkaTemplate;

    public PaymentService(PaymentRepository paymentRepository,
                          LedgerRepository ledgerRepository,
                          KafkaTemplate<String, PaymentRequestedEvent> kafkaTemplate) {
        this.paymentRepository = paymentRepository;
        this.ledgerRepository = ledgerRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    public Optional<Payment> getPayment(Long paymentId) {
        return paymentRepository.findById(paymentId);
    }

    public Payment createPayment(String idempotencyKey,
                                 Long fromAccount,
                                 Long toAccount,
                                 Long amount) {
        // 1. Check Idempotency Key
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) return existing.get();

        // 2. INSERT 1 line on payments
        Payment payment;
        try {
            payment = insertNewPayment(idempotencyKey);
        } catch(DataIntegrityViolationException e) {
            return paymentRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow();
        }

        // 3. Kafka Event Publish
        PaymentRequestedEvent event = new PaymentRequestedEvent(
                payment.getPaymentId(), fromAccount, toAccount, amount
        );
        kafkaTemplate.send(TOPIC, event);

        // 4. Instant Return (PENDING status)
        return payment;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected Payment insertNewPayment(String idempotencyKey) {
        Payment payment = new Payment(idempotencyKey);
        return paymentRepository.save(payment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment processLedgerAndFinish(Payment payment, Long fromAccount, Long toAccount, Long amount) {
        // Mock External Service
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Long paymentId = payment.getPaymentId();
        ledgerRepository.save(new LedgerEntry(paymentId, fromAccount, -amount));
        ledgerRepository.save(new LedgerEntry(paymentId, toAccount, amount));
        payment.setStatus("SUCCEEDED");
        return paymentRepository.save(payment);
    }

}