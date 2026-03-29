package com.paymentprocessor.payment.api.dto;

import java.util.List;

public record PagedPaymentResponse(
        List<PaymentResponse> payments,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
