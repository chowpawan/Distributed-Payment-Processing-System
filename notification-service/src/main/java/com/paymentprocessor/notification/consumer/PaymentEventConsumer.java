package com.paymentprocessor.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.events.PaymentCompleted;
import com.paymentprocessor.events.PaymentEvent;
import com.paymentprocessor.events.PaymentFailed;
import com.paymentprocessor.notification.handler.NotificationHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final List<NotificationHandler> handlers;
    private final ObjectMapper objectMapper;

    public PaymentEventConsumer(List<NotificationHandler> handlers, ObjectMapper objectMapper) {
        this.handlers = handlers;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${kafka.topics.payment-events}",
            groupId = "notification-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            PaymentEvent event = objectMapper.readValue(record.value(), PaymentEvent.class);

            if (event instanceof PaymentCompleted || event instanceof PaymentFailed) {
                String customerId = extractCustomerId(event);
                for (NotificationHandler handler : handlers) {
                    if (handler.supports(event)) {
                        try {
                            handler.handle(event, customerId);
                        } catch (Exception e) {
                            log.error("Handler {} failed for paymentId={}: {}",
                                    handler.getClass().getSimpleName(), event.paymentId(), e.getMessage(), e);
                        }
                    }
                }
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process Kafka record at offset={}: {}", record.offset(), e.getMessage(), e);
            // Don't ack — redelivery or DLQ
        }
    }

    private String extractCustomerId(PaymentEvent event) {
        if (event instanceof PaymentCompleted completed) {
            return completed.paymentId();
        } else if (event instanceof PaymentFailed failed) {
            return failed.paymentId();
        }
        return "unknown";
    }
}
