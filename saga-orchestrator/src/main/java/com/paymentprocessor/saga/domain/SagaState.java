package com.paymentprocessor.saga.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saga_state")
public class SagaState {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID sagaId;

    @Column(nullable = false, unique = true)
    private UUID paymentId;

    @Column(nullable = false)
    private String currentStep;

    @Column(nullable = false)
    private String status;

    private String lastError;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    private Instant completedAt;

    public UUID getSagaId() { return sagaId; }
    public void setSagaId(UUID sagaId) { this.sagaId = sagaId; }
    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
    public String getCurrentStep() { return currentStep; }
    public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
