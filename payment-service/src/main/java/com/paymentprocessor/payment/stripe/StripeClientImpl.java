package com.paymentprocessor.payment.stripe;

import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.param.RefundCreateParams;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class StripeClientImpl implements StripeClient {

    @Override
    public String createRefund(String chargeId, BigDecimal amount) {
        try {
            RefundCreateParams params = RefundCreateParams.builder()
                    .setCharge(chargeId)
                    .setAmount(amount.movePointRight(2).longValue())
                    .build();
            Refund refund = Refund.create(params);
            return refund.getId();
        } catch (StripeException e) {
            throw new StripeOperationException("Refund failed for charge " + chargeId, e);
        }
    }
}
