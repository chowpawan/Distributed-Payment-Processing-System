package com.paymentprocessor.recon.job;

import com.paymentprocessor.recon.domain.Payment;
import com.paymentprocessor.recon.domain.PaymentStatus;
import com.paymentprocessor.recon.repository.PaymentReconciliationRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class ReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationJob.class);
    private static final Duration PENDING_GRACE_PERIOD = Duration.ofMinutes(2);

    private final PaymentReconciliationRepository paymentRepository;
    private final ReconciliationProcessor processor;
    private final Counter reconRunCounter;

    public ReconciliationJob(PaymentReconciliationRepository paymentRepository,
                              ReconciliationProcessor processor,
                              MeterRegistry meterRegistry) {
        this.paymentRepository = paymentRepository;
        this.processor = processor;
        this.reconRunCounter = Counter.builder("recon.runs.total")
                .description("Total reconciliation job runs")
                .register(meterRegistry);
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void reconcile() {
        log.info("Starting reconciliation run");
        reconRunCounter.increment();

        Instant threshold = Instant.now().minus(PENDING_GRACE_PERIOD);
        List<Payment> stuckPayments = paymentRepository.findStuckPayments(
                PaymentStatus.PENDING, threshold);

        log.info("Found {} stuck PENDING payments to reconcile", stuckPayments.size());

        for (Payment payment : stuckPayments) {
            try {
                processor.reconcile(payment);
            } catch (Exception e) {
                // Log and continue — one failure should not block the rest of the batch
                log.error("Reconciliation failed for paymentId={}: {}",
                        payment.getId(), e.getMessage(), e);
            }
        }

        log.info("Reconciliation run complete");
    }
}
