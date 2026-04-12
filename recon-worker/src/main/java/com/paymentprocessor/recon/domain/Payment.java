package com.paymentprocessor.recon.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Read/write projection of the payments table for reconciliation.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private String customerId;

    private String stripeChargeId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public BigDecimal getAmount() { return amount; }
    public String getCustomerId() { return customerId; }
    public String getStripeChargeId() { return stripeChargeId; }
    public void setStripeChargeId(String stripeChargeId) { this.stripeChargeId = stripeChargeId; }
    public Instant getCreatedAt() { return createdAt; }
}
