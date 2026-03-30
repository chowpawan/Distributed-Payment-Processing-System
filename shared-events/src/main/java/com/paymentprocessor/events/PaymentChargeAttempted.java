package com.paymentprocessor.events;

import java.time.Instant;

public record PaymentChargeAttempted(
        String paymentId,
        int attemptNumber,
        String stripeChargeId,
        Instant occurredAt,
        String correlationId
) implements PaymentEvent {

    @Override
    public EventType eventType() {
        return EventType.PAYMENT_CHARGE_ATTEMPTED;
    }
}
