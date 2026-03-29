package com.paymentprocessor.payment.repository;

import com.paymentprocessor.payment.domain.Payment;
import com.paymentprocessor.payment.domain.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Page<Payment> findByCustomerId(String customerId, Pageable pageable);

    boolean existsByIdAndStatus(UUID id, PaymentStatus status);
}
