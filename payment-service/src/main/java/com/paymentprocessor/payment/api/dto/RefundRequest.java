package com.paymentprocessor.payment.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RefundRequest(

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "refund amount must be positive")
        BigDecimal amount,

        String reason
) {}
