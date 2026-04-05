package com.paymentprocessor.saga.statemachine;

public enum SagaStep {
    CHARGE,
    RECORD,
    NOTIFY,
    COMPLETED,
    FAILED
}
