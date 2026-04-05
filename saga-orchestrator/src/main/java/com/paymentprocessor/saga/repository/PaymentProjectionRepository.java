package com.paymentprocessor.saga.repository;

import com.paymentprocessor.saga.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentProjectionRepository extends JpaRepository<Payment, UUID> {}
