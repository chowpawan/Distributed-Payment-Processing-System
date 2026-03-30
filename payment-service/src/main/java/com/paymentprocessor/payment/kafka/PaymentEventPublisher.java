package com.paymentprocessor.payment.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.events.PaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public PaymentEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                  ObjectMapper objectMapper,
                                  @Value("${kafka.topics.payment-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    public void publish(PaymentEvent event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, event.paymentId(), json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish event type={} paymentId={}",
                                    event.eventType(), event.paymentId(), ex);
                        } else {
                            log.debug("Published event type={} paymentId={} offset={}",
                                    event.eventType(), event.paymentId(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to serialize event for paymentId={}", event.paymentId(), e);
        }
    }
}
