package com.paymentprocessor.notification.handler;

import com.paymentprocessor.events.PaymentEvent;

public interface NotificationHandler {

    boolean supports(PaymentEvent event);

    void handle(PaymentEvent event, String customerId);
}
