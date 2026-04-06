package com.paymentprocessor.saga.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.events.PaymentEvent;
import com.paymentprocessor.events.PaymentInitiated;
import com.paymentprocessor.saga.statemachine.SagaOrchestrator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class PaymentInitiatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentInitiatedConsumer.class);

    private final SagaOrchestrator sagaOrchestrator;
    private final ObjectMapper objectMapper;

    public PaymentInitiatedConsumer(SagaOrchestrator sagaOrchestrator, ObjectMapper objectMapper) {
        this.sagaOrchestrator = sagaOrchestrator;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${kafka.topics.payment-events}",
            groupId = "saga-orchestrator",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            PaymentEvent event = objectMapper.readValue(record.value(), PaymentEvent.class);

            if (event instanceof PaymentInitiated initiated) {
                sagaOrchestrator.execute(initiated);
                ack.acknowledge();
            } else {
                // Not our event type — acknowledge and skip
                ack.acknowledge();
            }
        } catch (Exception e) {
            log.error("Failed to process Kafka record at offset={} partition={}: {}",
                    record.offset(), record.partition(), e.getMessage(), e);
            // Do not ack — Kafka will redeliver after max.poll.interval.ms
            // In production, implement a DLQ after N retries
        }
    }
}
