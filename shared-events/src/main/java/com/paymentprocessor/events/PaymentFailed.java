package com.paymentprocessor.events;

import java.time.Instant;

public record PaymentFailed(
        String paymentId,
        String reason,
        String stripeErrorCode,
        Instant occurredAt,
        String correlationId
) implements PaymentEvent {

    @Override
    public EventType eventType() {
        return EventType.PAYMENT_FAILED;
    }
}
