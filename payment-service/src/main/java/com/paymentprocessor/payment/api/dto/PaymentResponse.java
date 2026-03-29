package com.paymentprocessor.payment.api.dto;

import com.paymentprocessor.payment.domain.Payment;
import com.paymentprocessor.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        String customerId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String stripeChargeId,
        Map<String, String> metadata,
        Instant createdAt,
        Instant updatedAt
) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(
                p.getId(),
                p.getCustomerId(),
                p.getAmount(),
                p.getCurrency(),
                p.getStatus(),
                p.getStripeChargeId(),
                p.getMetadata(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
