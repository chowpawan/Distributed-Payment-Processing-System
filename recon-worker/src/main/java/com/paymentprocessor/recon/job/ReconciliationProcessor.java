package com.paymentprocessor.recon.job;

import com.paymentprocessor.recon.domain.Payment;
import com.paymentprocessor.recon.domain.PaymentStatus;
import com.paymentprocessor.recon.domain.ReconAuditLog;
import com.paymentprocessor.recon.repository.PaymentReconciliationRepository;
import com.paymentprocessor.recon.repository.ReconAuditLogRepository;
import com.paymentprocessor.recon.stripe.StripeReconciliationClient;
import com.stripe.model.Charge;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class ReconciliationProcessor {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationProcessor.class);
    private static final Duration STALE_THRESHOLD = Duration.ofMinutes(30);

    private final PaymentReconciliationRepository paymentRepository;
    private final ReconAuditLogRepository auditLogRepository;
    private final StripeReconciliationClient stripeClient;
    private final Counter discrepancyCounter;

    public ReconciliationProcessor(PaymentReconciliationRepository paymentRepository,
                                    ReconAuditLogRepository auditLogRepository,
                                    StripeReconciliationClient stripeClient,
                                    MeterRegistry meterRegistry) {
        this.paymentRepository = paymentRepository;
        this.auditLogRepository = auditLogRepository;
        this.stripeClient = stripeClient;
        this.discrepancyCounter = Counter.builder("recon.discrepancies.total")
                .description("Total reconciliation discrepancies resolved")
                .register(meterRegistry);
    }

    @Transactional
    public void reconcile(Payment payment) {
        String priorStatus = payment.getStatus().name();
        Optional<Charge> stripeCharge = stripeClient.findChargeByPaymentId(payment.getId().toString());

        String stripeStatus = null;
        String actionTaken;

        if (stripeCharge.isEmpty()) {
            Duration age = Duration.between(payment.getCreatedAt(), Instant.now());
            if (age.compareTo(STALE_THRESHOLD) > 0) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                actionTaken = "MARKED_FAILED_NO_STRIPE_RECORD";
                discrepancyCounter.increment();
                log.warn("Payment {} has no Stripe record after {} min, marked FAILED",
                        payment.getId(), age.toMinutes());
            } else {
                actionTaken = "NO_ACTION_RECENT_PAYMENT";
                log.debug("Payment {} has no Stripe record but is only {} min old, skipping",
                        payment.getId(), age.toMinutes());
            }
        } else {
            Charge charge = stripeCharge.get();
            stripeStatus = charge.getStatus();

            switch (charge.getStatus()) {
                case "succeeded" -> {
                    payment.setStatus(PaymentStatus.COMPLETED);
                    payment.setStripeChargeId(charge.getId());
                    paymentRepository.save(payment);
                    actionTaken = "MARKED_COMPLETED";
                    discrepancyCounter.increment();
                    log.info("Payment {} reconciled: local=PENDING stripe=succeeded → COMPLETED",
                            payment.getId());
                }
                case "failed" -> {
                    payment.setStatus(PaymentStatus.FAILED);
                    paymentRepository.save(payment);
                    actionTaken = "MARKED_FAILED";
                    discrepancyCounter.increment();
                    log.info("Payment {} reconciled: local=PENDING stripe=failed → FAILED",
                            payment.getId());
                }
                default -> {
                    actionTaken = "NO_ACTION_STRIPE_STATUS_" + charge.getStatus().toUpperCase();
                    log.debug("Payment {} has Stripe status={}, no action taken",
                            payment.getId(), charge.getStatus());
                }
            }
        }

        ReconAuditLog auditLog = new ReconAuditLog();
        auditLog.setPaymentId(payment.getId());
        auditLog.setLocalStatus(priorStatus);
        auditLog.setStripeStatus(stripeStatus);
        auditLog.setActionTaken(actionTaken);
        auditLogRepository.save(auditLog);
    }
}
