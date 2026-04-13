package com.paymentprocessor.notification.webhook;

import com.paymentprocessor.notification.domain.WebhookSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

@Service
public class WebhookDispatcher {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatcher.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration INITIAL_DELAY = Duration.ofSeconds(1);

    private final RestClient restClient;
    private final WebhookSignatureSigner signer;

    public WebhookDispatcher(RestClient restClient, WebhookSignatureSigner signer) {
        this.restClient = restClient;
        this.signer = signer;
    }

    public void dispatch(WebhookSubscription subscription, String eventPayload) {
        String signature = signer.sign(eventPayload, subscription.getSecret());

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                restClient.post()
                        .uri(subscription.getUrl())
                        .header("X-Webhook-Signature", signature)
                        .header("Content-Type", "application/json")
                        .header("X-Delivery-Attempt", String.valueOf(attempt))
                        .body(eventPayload)
                        .retrieve()
                        .toBodilessEntity();

                log.info("Webhook delivered to subscriptionId={} attempt={}/{}",
                        subscription.getId(), attempt, MAX_ATTEMPTS);
                return;

            } catch (RestClientException e) {
                log.warn("Webhook attempt {}/{} failed for subscriptionId={}: {}",
                        attempt, MAX_ATTEMPTS, subscription.getId(), e.getMessage());

                if (attempt < MAX_ATTEMPTS) {
                    // Exponential backoff: 1s, 2s, 4s, 8s
                    long delayMs = INITIAL_DELAY.toMillis() * (1L << (attempt - 1));
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Webhook delivery interrupted for subscriptionId={}",
                                subscription.getId());
                        return;
                    }
                }
            }
        }

        log.error("Webhook delivery permanently failed for subscriptionId={} after {} attempts",
                subscription.getId(), MAX_ATTEMPTS);
        // Production improvement: write to a failed_deliveries table for manual retry
    }
}
