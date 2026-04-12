package com.paymentprocessor.recon.repository;

import com.paymentprocessor.recon.domain.Payment;
import com.paymentprocessor.recon.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PaymentReconciliationRepository extends JpaRepository<Payment, UUID> {

    @Query("""
            SELECT p FROM Payment p
            WHERE p.status = :status
            AND p.createdAt < :threshold
            ORDER BY p.createdAt ASC
            """)
    List<Payment> findStuckPayments(@Param("status") PaymentStatus status,
                                    @Param("threshold") Instant threshold);
}
