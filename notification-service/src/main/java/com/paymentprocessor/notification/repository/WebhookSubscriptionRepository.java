package com.paymentprocessor.notification.repository;

import com.paymentprocessor.notification.domain.WebhookSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, UUID> {

    @Query("""
            SELECT ws FROM WebhookSubscription ws
            JOIN ws.events e
            WHERE ws.customerId = :customerId
            AND ws.active = true
            AND e = :eventType
            """)
    List<WebhookSubscription> findActiveByCustomerAndEventType(
            @Param("customerId") String customerId,
            @Param("eventType") String eventType);
}
