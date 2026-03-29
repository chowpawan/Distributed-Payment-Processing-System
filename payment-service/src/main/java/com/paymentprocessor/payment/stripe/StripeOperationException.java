package com.paymentprocessor.payment.stripe;

public class StripeOperationException extends RuntimeException {

    public StripeOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
