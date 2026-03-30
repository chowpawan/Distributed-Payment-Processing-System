package com.paymentprocessor.events;

public enum EventType {
    PAYMENT_INITIATED,
    PAYMENT_CHARGE_ATTEMPTED,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    PAYMENT_REFUNDED
}
