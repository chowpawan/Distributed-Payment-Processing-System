package com.paymentprocessor.payment.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class PaymentMetrics {

    private final Counter paymentsCreated;
    private final Counter paymentsRefunded;
    private final Counter idempotencyHits;

    public PaymentMetrics(MeterRegistry registry) {
        this.paymentsCreated = Counter.builder("payments.created.total")
                .description("Total number of payments created")
                .register(registry);
        this.paymentsRefunded = Counter.builder("payments.refunded.total")
                .description("Total number of payments refunded")
                .register(registry);
        this.idempotencyHits = Counter.builder("payments.idempotency.hits.total")
                .description("Total idempotency cache hits")
                .register(registry);
    }

    public void paymentCreated() { paymentsCreated.increment(); }
    public void paymentRefunded() { paymentsRefunded.increment(); }
    public void idempotencyHit() { idempotencyHits.increment(); }
}
