package com.paymentprocessor.saga.compensation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.events.PaymentFailed;
import com.paymentprocessor.saga.domain.Payment;
import com.paymentprocessor.saga.domain.PaymentStatus;
import com.paymentprocessor.saga.repository.PaymentProjectionRepository;
import com.paymentprocessor.saga.statemachine.SagaContext;
import com.paymentprocessor.saga.steps.SagaStepException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
public class CompensationHandler {

    private static final Logger log = LoggerFactory.getLogger(CompensationHandler.class);

    private final PaymentProjectionRepository paymentRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public CompensationHandler(PaymentProjectionRepository paymentRepository,
                                KafkaTemplate<String, String> kafkaTemplate,
                                ObjectMapper objectMapper,
                                @Value("${kafka.topics.payment-events}") String topic) {
        this.paymentRepository = paymentRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    @Transactional
    public void compensate(SagaContext ctx, SagaStepException cause) {
        log.warn("Compensating saga for paymentId={} reason={}", ctx.paymentId(), cause.getMessage());

        // Mark payment as FAILED in the shared DB
        try {
            Payment payment = paymentRepository.findById(UUID.fromString(ctx.paymentId()))
                    .orElse(null);
            if (payment != null) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
            }
        } catch (Exception e) {
            log.error("Failed to update payment status during compensation for paymentId={}",
                    ctx.paymentId(), e);
        }

        // Publish PaymentFailed event so notification-service can alert the customer
        try {
            PaymentFailed event = new PaymentFailed(
                    ctx.paymentId(),
                    cause.getMessage(),
                    null,
                    Instant.now(),
                    ctx.correlationId()
            );
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, ctx.paymentId(), json);
            log.info("PaymentFailed event published for paymentId={}", ctx.paymentId());
        } catch (Exception e) {
            log.error("Failed to publish PaymentFailed event during compensation for paymentId={}",
                    ctx.paymentId(), e);
        }
    }
}
