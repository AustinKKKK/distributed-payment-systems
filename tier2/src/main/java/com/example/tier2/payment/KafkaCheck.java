package com.example.tier2.payment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaCheck {
    @Autowired(required = false)
    private KafkaTemplate<?, ?> kafkaTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void check() {
        System.out.println("=== KAFKA TEMPLATE CHECK: " + (kafkaTemplate != null ? "FOUND" : "NULL") + " ===");
    }
}