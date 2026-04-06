package com.paymentprocessor.saga.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.events.PaymentCompleted;
import com.paymentprocessor.saga.statemachine.SagaContext;
import com.paymentprocessor.saga.statemachine.SagaStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class NotifyStep implements SagaStepHandler {

    private static final Logger log = LoggerFactory.getLogger(NotifyStep.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public NotifyStep(KafkaTemplate<String, String> kafkaTemplate,
                      ObjectMapper objectMapper,
                      @Value("${kafka.topics.payment-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Override
    public SagaStep handles() {
        return SagaStep.NOTIFY;
    }

    @Override
    public SagaContext execute(SagaContext ctx) throws SagaStepException {
        PaymentCompleted event = new PaymentCompleted(
                ctx.paymentId(),
                ctx.stripeChargeId(),
                ctx.amount(),
                ctx.currency(),
                Instant.now(),
                ctx.correlationId()
        );

        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, ctx.paymentId(), json);
            log.info("PaymentCompleted event published for paymentId={}", ctx.paymentId());
            return ctx;
        } catch (Exception e) {
            throw new SagaStepException("Failed to publish PaymentCompleted for paymentId="
                    + ctx.paymentId(), e);
        }
    }
}
