package com.paymentprocessor.events;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentInitiated(
        String paymentId,
        String customerId,
        BigDecimal amount,
        String currency,
        String idempotencyKey,
        Instant occurredAt,
        String correlationId
) implements PaymentEvent {

    @Override
    public EventType eventType() {
        return EventType.PAYMENT_INITIATED;
    }
}
