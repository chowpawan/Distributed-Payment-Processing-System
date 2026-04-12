package com.paymentprocessor.recon.stripe;

import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.ChargeSearchResult;
import com.stripe.param.ChargeSearchParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class StripeReconciliationClient {

    private static final Logger log = LoggerFactory.getLogger(StripeReconciliationClient.class);

    /**
     * Searches Stripe for charges with the given payment_id in metadata.
     * Returns the first matching charge, or empty if none found.
     */
    public Optional<Charge> findChargeByPaymentId(String paymentId) {
        try {
            ChargeSearchResult result = Charge.search(
                    ChargeSearchParams.builder()
                            .setQuery("metadata['payment_id']:'" + paymentId + "'")
                            .build()
            );
            List<Charge> charges = result.getData();
            if (charges.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(charges.get(0));
        } catch (StripeException e) {
            log.error("Stripe search failed for paymentId={}: {}", paymentId, e.getMessage());
            return Optional.empty();
        }
    }
}
