package com.paymentprocessor.payment.stripe;

import java.math.BigDecimal;

public interface StripeClient {

    String createRefund(String chargeId, BigDecimal amount);
}
