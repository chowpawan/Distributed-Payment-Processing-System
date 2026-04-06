package com.paymentprocessor.saga.steps;

import com.paymentprocessor.saga.domain.Payment;
import com.paymentprocessor.saga.domain.PaymentStatus;
import com.paymentprocessor.saga.repository.PaymentProjectionRepository;
import com.paymentprocessor.saga.statemachine.SagaContext;
import com.paymentprocessor.saga.statemachine.SagaStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class RecordStep implements SagaStepHandler {

    private static final Logger log = LoggerFactory.getLogger(RecordStep.class);

    private final PaymentProjectionRepository paymentRepository;

    public RecordStep(PaymentProjectionRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public SagaStep handles() {
        return SagaStep.RECORD;
    }

    @Override
    @Transactional
    public SagaContext execute(SagaContext ctx) throws SagaStepException {
        Payment payment = paymentRepository.findById(UUID.fromString(ctx.paymentId()))
                .orElseThrow(() -> new SagaStepException(
                        "Payment not found in DB: " + ctx.paymentId()));

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setStripeChargeId(ctx.stripeChargeId());
        paymentRepository.save(payment);

        log.info("Payment status updated to COMPLETED: paymentId={} chargeId={}",
                ctx.paymentId(), ctx.stripeChargeId());
        return ctx;
    }
}
