package com.paymentprocessor.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.events.config.EventsObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PaymentEventSerializationTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = EventsObjectMapper.create();
    }

    @Test
    void paymentInitiated_roundTrips() throws Exception {
        PaymentInitiated original = new PaymentInitiated(
                UUID.randomUUID().toString(),
                "cust_001",
                new BigDecimal("99.99"),
                "usd",
                "idem-key-001",
                Instant.parse("2024-01-15T10:00:00Z"),
                UUID.randomUUID().toString()
        );

        String json = mapper.writeValueAsString(original);
        PaymentEvent deserialized = mapper.readValue(json, PaymentEvent.class);

        assertInstanceOf(PaymentInitiated.class, deserialized);
        PaymentInitiated result = (PaymentInitiated) deserialized;
        assertEquals(original.paymentId(), result.paymentId());
        assertEquals(original.customerId(), result.customerId());
        assertEquals(0, original.amount().compareTo(result.amount()));
        assertEquals(original.currency(), result.currency());
        assertEquals(original.idempotencyKey(), result.idempotencyKey());
        assertEquals(original.occurredAt(), result.occurredAt());
        assertEquals(original.correlationId(), result.correlationId());
    }

    @Test
    void paymentCompleted_roundTrips() throws Exception {
        PaymentCompleted original = new PaymentCompleted(
                UUID.randomUUID().toString(),
                "ch_stripe_001",
                new BigDecimal("99.99"),
                "usd",
                Instant.now(),
                UUID.randomUUID().toString()
        );

        String json = mapper.writeValueAsString(original);
        PaymentEvent deserialized = mapper.readValue(json, PaymentEvent.class);

        assertInstanceOf(PaymentCompleted.class, deserialized);
        PaymentCompleted result = (PaymentCompleted) deserialized;
        assertEquals(original.stripeChargeId(), result.stripeChargeId());
    }

    @Test
    void paymentFailed_roundTrips() throws Exception {
        PaymentFailed original = new PaymentFailed(
                UUID.randomUUID().toString(),
                "card_declined",
                "insufficient_funds",
                Instant.now(),
                UUID.randomUUID().toString()
        );

        String json = mapper.writeValueAsString(original);
        PaymentEvent deserialized = mapper.readValue(json, PaymentEvent.class);

        assertInstanceOf(PaymentFailed.class, deserialized);
        assertEquals(original.reason(), ((PaymentFailed) deserialized).reason());
        assertEquals(original.stripeErrorCode(), ((PaymentFailed) deserialized).stripeErrorCode());
    }

    @Test
    void paymentFailed_nullStripeErrorCode_roundTrips() throws Exception {
        PaymentFailed original = new PaymentFailed(
                UUID.randomUUID().toString(),
                "timeout",
                null,
                Instant.now(),
                UUID.randomUUID().toString()
        );

        String json = mapper.writeValueAsString(original);
        PaymentEvent deserialized = mapper.readValue(json, PaymentEvent.class);

        assertInstanceOf(PaymentFailed.class, deserialized);
        assertNull(((PaymentFailed) deserialized).stripeErrorCode());
    }

    @Test
    void paymentRefunded_roundTrips() throws Exception {
        PaymentRefunded original = new PaymentRefunded(
                UUID.randomUUID().toString(),
                "re_stripe_001",
                new BigDecimal("50.00"),
                Instant.now(),
                UUID.randomUUID().toString()
        );

        String json = mapper.writeValueAsString(original);
        PaymentEvent deserialized = mapper.readValue(json, PaymentEvent.class);

        assertInstanceOf(PaymentRefunded.class, deserialized);
        assertEquals(original.stripeRefundId(), ((PaymentRefunded) deserialized).stripeRefundId());
    }

    @Test
    void paymentChargeAttempted_roundTrips() throws Exception {
        PaymentChargeAttempted original = new PaymentChargeAttempted(
                UUID.randomUUID().toString(),
                1,
                null,
                Instant.now(),
                UUID.randomUUID().toString()
        );

        String json = mapper.writeValueAsString(original);
        PaymentEvent deserialized = mapper.readValue(json, PaymentEvent.class);

        assertInstanceOf(PaymentChargeAttempted.class, deserialized);
        assertEquals(original.attemptNumber(), ((PaymentChargeAttempted) deserialized).attemptNumber());
    }

    @Test
    void serializedJson_containsEventTypeField() throws Exception {
        PaymentInitiated event = new PaymentInitiated(
                "pay-001", "cust-001", BigDecimal.TEN, "usd",
                "key-001", Instant.now(), "corr-001"
        );
        String json = mapper.writeValueAsString(event);
        assertTrue(json.contains("\"eventType\":\"PAYMENT_INITIATED\""));
    }
}
