package com.paymentprocessor.payment.service;

import java.util.Optional;

public interface IdempotencyService {

    /**
     * Returns cached response if the idempotency key has been seen before.
     */
    Optional<IdempotencyResponse> get(String customerId, String idempotencyKey);

    /**
     * Stores the response for a given idempotency key.
     * No-op if the key already exists (SETNX semantics).
     */
    void store(String customerId, String idempotencyKey, IdempotencyResponse response);

    record IdempotencyResponse(int status, String body) {}
}
