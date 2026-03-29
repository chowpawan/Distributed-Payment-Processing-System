package com.paymentprocessor.payment.api.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Map;

public record CreatePaymentRequest(

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be positive")
        BigDecimal amount,

        @NotBlank(message = "currency is required")
        @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO code")
        String currency,

        Map<String, String> metadata
) {}
