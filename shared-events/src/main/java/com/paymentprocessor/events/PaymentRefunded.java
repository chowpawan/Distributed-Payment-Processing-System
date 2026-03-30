package com.paymentprocessor.events;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentRefunded(
        String paymentId,
        String stripeRefundId,
        BigDecimal refundAmount,
        Instant occurredAt,
        String correlationId
) implements PaymentEvent {

    @Override
    public EventType eventType() {
        return EventType.PAYMENT_REFUNDED;
    }
}
