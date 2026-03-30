package com.paymentprocessor.events;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;

public sealed interface PaymentEvent permits
        PaymentInitiated,
        PaymentChargeAttempted,
        PaymentCompleted,
        PaymentFailed,
        PaymentRefunded {

    String paymentId();

    Instant occurredAt();

    String correlationId();

    @JsonIgnore
    EventType eventType();
}
