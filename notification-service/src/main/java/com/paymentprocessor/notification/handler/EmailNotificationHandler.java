package com.paymentprocessor.notification.handler;

import com.paymentprocessor.events.PaymentCompleted;
import com.paymentprocessor.events.PaymentEvent;
import com.paymentprocessor.events.PaymentFailed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationHandler implements NotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationHandler.class);

    @Override
    public boolean supports(PaymentEvent event) {
        return event instanceof PaymentCompleted || event instanceof PaymentFailed;
    }

    @Override
    public void handle(PaymentEvent event, String customerId) {
        // Mock implementation — logs instead of sending real email
        // Production: integrate with SES/SendGrid/Mailgun
        switch (event) {
            case PaymentCompleted completed -> log.info(
                    "[EMAIL MOCK] Sending payment confirmation to customer={} paymentId={} amount={} {}",
                    customerId, completed.paymentId(), completed.amount(), completed.currency()
            );
            case PaymentFailed failed -> log.info(
                    "[EMAIL MOCK] Sending payment failure notification to customer={} paymentId={} reason={}",
                    customerId, failed.paymentId(), failed.reason()
            );
            default -> log.debug("[EMAIL MOCK] No email template for event type={}", event.eventType());
        }
    }
}
