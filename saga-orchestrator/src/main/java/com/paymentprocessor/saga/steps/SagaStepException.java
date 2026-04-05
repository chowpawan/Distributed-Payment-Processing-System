package com.paymentprocessor.saga.steps;

public class SagaStepException extends Exception {

    public SagaStepException(String message) {
        super(message);
    }

    public SagaStepException(String message, Throwable cause) {
        super(message, cause);
    }
}
