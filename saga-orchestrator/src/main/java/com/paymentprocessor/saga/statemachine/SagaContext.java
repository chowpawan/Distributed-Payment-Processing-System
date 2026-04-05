package com.paymentprocessor.saga.statemachine;

import com.paymentprocessor.events.PaymentInitiated;

import java.math.BigDecimal;

public record SagaContext(
        String sagaId,
        String paymentId,
        String customerId,
        BigDecimal amount,
        String currency,
        String stripeChargeId,
        String correlationId,
        SagaStep currentStep,
        SagaStatus status
) {
    public static SagaContext from(PaymentInitiated event, String sagaId) {
        return new SagaContext(
                sagaId,
                event.paymentId(),
                event.customerId(),
                event.amount(),
                event.currency(),
                null,
                event.correlationId(),
                SagaStep.CHARGE,
                SagaStatus.RUNNING
        );
    }

    public SagaContext withChargeId(String stripeChargeId) {
        return new SagaContext(sagaId, paymentId, customerId, amount, currency,
                stripeChargeId, correlationId, currentStep, status);
    }

    public SagaContext withStep(SagaStep step) {
        return new SagaContext(sagaId, paymentId, customerId, amount, currency,
                stripeChargeId, correlationId, step, status);
    }

    public SagaContext withStatus(SagaStatus status) {
        return new SagaContext(sagaId, paymentId, customerId, amount, currency,
                stripeChargeId, correlationId, currentStep, status);
    }
}
