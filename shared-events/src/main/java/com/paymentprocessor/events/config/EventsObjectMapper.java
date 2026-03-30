package com.paymentprocessor.events.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.paymentprocessor.events.*;

public final class EventsObjectMapper {

    private EventsObjectMapper() {}

    public static ObjectMapper create() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        configure(mapper);
        return mapper;
    }

    /**
     * Applies payment event polymorphic config to an existing ObjectMapper.
     * Use this in Spring services to configure the auto-configured ObjectMapper.
     */
    public static void configure(ObjectMapper mapper) {
        mapper.addMixIn(PaymentEvent.class, PaymentEventMixin.class);
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = PaymentInitiated.class, name = "PAYMENT_INITIATED"),
        @JsonSubTypes.Type(value = PaymentChargeAttempted.class, name = "PAYMENT_CHARGE_ATTEMPTED"),
        @JsonSubTypes.Type(value = PaymentCompleted.class, name = "PAYMENT_COMPLETED"),
        @JsonSubTypes.Type(value = PaymentFailed.class, name = "PAYMENT_FAILED"),
        @JsonSubTypes.Type(value = PaymentRefunded.class, name = "PAYMENT_REFUNDED"),
    })
    public abstract static class PaymentEventMixin {}
}
