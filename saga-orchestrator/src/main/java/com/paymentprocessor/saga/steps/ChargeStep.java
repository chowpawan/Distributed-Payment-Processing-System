package com.paymentprocessor.saga.steps;

import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.ChargeSearchResult;
import com.stripe.param.ChargeCreateParams;
import com.stripe.param.ChargeSearchParams;
import com.paymentprocessor.saga.statemachine.SagaContext;
import com.paymentprocessor.saga.statemachine.SagaStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ChargeStep implements SagaStepHandler {

    private static final Logger log = LoggerFactory.getLogger(ChargeStep.class);

    @Override
    public SagaStep handles() {
        return SagaStep.CHARGE;
    }

    @Override
    public SagaContext execute(SagaContext ctx) throws SagaStepException {
        // Check if Stripe already has a charge for this payment (idempotency on retry/redelivery)
        try {
            ChargeSearchResult existing = Charge.search(
                    ChargeSearchParams.builder()
                            .setQuery("metadata['payment_id']:'" + ctx.paymentId() + "'")
                            .build()
            );

            if (!existing.getData().isEmpty()) {
                String existingChargeId = existing.getData().get(0).getId();
                log.info("Found existing Stripe charge {} for paymentId={}", existingChargeId, ctx.paymentId());
                return ctx.withChargeId(existingChargeId);
            }
        } catch (StripeException e) {
            log.warn("Stripe search failed for paymentId={}, proceeding with new charge: {}",
                    ctx.paymentId(), e.getMessage());
        }

        // Create new charge with payment_id in metadata for future idempotency checks
        try {
            ChargeCreateParams params = ChargeCreateParams.builder()
                    .setAmount(ctx.amount().movePointRight(2).longValue())
                    .setCurrency(ctx.currency())
                    .putMetadata("payment_id", ctx.paymentId())
                    .putMetadata("correlation_id", ctx.correlationId())
                    .setSource("tok_visa") // In prod: customer's payment method token
                    .build();

            Charge charge = Charge.create(params);
            log.info("Stripe charge created: {} for paymentId={}", charge.getId(), ctx.paymentId());
            return ctx.withChargeId(charge.getId());

        } catch (StripeException e) {
            throw new SagaStepException("Stripe charge failed for paymentId=" + ctx.paymentId()
                    + ": " + e.getMessage(), e);
        }
    }
}
