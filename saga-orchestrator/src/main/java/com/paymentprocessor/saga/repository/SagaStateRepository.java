package com.paymentprocessor.saga.repository;

import com.paymentprocessor.saga.domain.SagaState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SagaStateRepository extends JpaRepository<SagaState, UUID> {

    boolean existsByPaymentId(UUID paymentId);
}
