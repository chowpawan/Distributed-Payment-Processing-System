package com.paymentprocessor.notification.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.events.PaymentCompleted;
import com.paymentprocessor.events.PaymentEvent;
import com.paymentprocessor.events.PaymentFailed;
import com.paymentprocessor.notification.domain.WebhookSubscription;
import com.paymentprocessor.notification.repository.WebhookSubscriptionRepository;
import com.paymentprocessor.notification.webhook.WebhookDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WebhookNotificationHandler implements NotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(WebhookNotificationHandler.class);

    private final WebhookSubscriptionRepository subscriptionRepository;
    private final WebhookDispatcher webhookDispatcher;
    private final ObjectMapper objectMapper;

    public WebhookNotificationHandler(WebhookSubscriptionRepository subscriptionRepository,
                                       WebhookDispatcher webhookDispatcher,
                                       ObjectMapper objectMapper) {
        this.subscriptionRepository = subscriptionRepository;
        this.webhookDispatcher = webhookDispatcher;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(PaymentEvent event) {
        return event instanceof PaymentCompleted || event instanceof PaymentFailed;
    }

    @Override
    public void handle(PaymentEvent event, String customerId) {
        String eventTypeName = event.eventType().name();
        List<WebhookSubscription> subscriptions =
                subscriptionRepository.findActiveByCustomerAndEventType(customerId, eventTypeName);

        if (subscriptions.isEmpty()) {
            log.debug("No webhook subscriptions for customer={} eventType={}", customerId, eventTypeName);
            return;
        }

        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            log.error("Failed to serialize event for webhook delivery: {}", e.getMessage());
            return;
        }

        for (WebhookSubscription subscription : subscriptions) {
            webhookDispatcher.dispatch(subscription, payload);
        }
    }
}
