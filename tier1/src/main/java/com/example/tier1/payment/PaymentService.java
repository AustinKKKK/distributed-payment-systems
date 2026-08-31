package com.example.tier1.payment;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final LedgerRepository ledgerRepository;

    public PaymentService(PaymentRepository paymentRepository,
                          LedgerRepository ledgerRepository) {
        this.paymentRepository = paymentRepository;
        this.ledgerRepository = ledgerRepository;
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

        /* 3. INSERT 2 lines on ledger_entries
           4. Change status to SUCCEEDED
           5. Return
         */
        return processLedgerAndFinish(payment, fromAccount, toAccount, amount);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected Payment insertNewPayment(String idempotencyKey) {
        Payment payment = new Payment(idempotencyKey);
        return paymentRepository.save(payment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected Payment processLedgerAndFinish(Payment payment, Long fromAccount, Long toAccount, Long amount) {

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
        return payment;
    }

}