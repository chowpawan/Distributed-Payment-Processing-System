package com.paymentprocessor.recon.repository;

import com.paymentprocessor.recon.domain.ReconAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReconAuditLogRepository extends JpaRepository<ReconAuditLog, UUID> {}
