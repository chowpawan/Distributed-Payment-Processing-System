package com.paymentprocessor.recon.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recon_audit_log")
public class ReconAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID paymentId;

    @Column(nullable = false)
    private String localStatus;

    private String stripeStatus;

    @Column(nullable = false)
    private String actionTaken;

    private String notes;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant reconciledAt;

    public UUID getId() { return id; }
    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
    public String getLocalStatus() { return localStatus; }
    public void setLocalStatus(String localStatus) { this.localStatus = localStatus; }
    public String getStripeStatus() { return stripeStatus; }
    public void setStripeStatus(String stripeStatus) { this.stripeStatus = stripeStatus; }
    public String getActionTaken() { return actionTaken; }
    public void setActionTaken(String actionTaken) { this.actionTaken = actionTaken; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getReconciledAt() { return reconciledAt; }
}
