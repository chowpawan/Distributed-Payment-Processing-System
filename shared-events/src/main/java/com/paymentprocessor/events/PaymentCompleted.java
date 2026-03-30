package com.paymentprocessor.events;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentCompleted(
        String paymentId,
        String stripeChargeId,
        BigDecimal amount,
        String currency,
        Instant occurredAt,
        String correlationId
) implements PaymentEvent {

    @Override
    public EventType eventType() {
        return EventType.PAYMENT_COMPLETED;
    }
}
