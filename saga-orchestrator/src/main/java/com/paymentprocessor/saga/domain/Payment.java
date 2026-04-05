package com.paymentprocessor.saga.domain;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * Minimal projection of the payments table for the saga-orchestrator.
 * Owns only the fields it needs to update during saga execution.
 */
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private String stripeChargeId;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public String getStripeChargeId() { return stripeChargeId; }
    public void setStripeChargeId(String stripeChargeId) { this.stripeChargeId = stripeChargeId; }
}
