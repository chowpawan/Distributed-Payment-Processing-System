package com.paymentprocessor.saga.statemachine;

public enum SagaStatus {
    RUNNING,
    COMPENSATING,
    COMPLETED,
    FAILED
}
