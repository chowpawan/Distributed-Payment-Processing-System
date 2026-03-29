package com.paymentprocessor.payment.service;

import com.paymentprocessor.payment.api.dto.CreatePaymentRequest;
import com.paymentprocessor.payment.api.dto.PagedPaymentResponse;
import com.paymentprocessor.payment.api.dto.PaymentResponse;
import com.paymentprocessor.payment.api.dto.RefundRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PaymentService {

    PaymentResponse createPayment(String customerId, String idempotencyKey, CreatePaymentRequest request);

    PaymentResponse getPayment(UUID id);

    PagedPaymentResponse listPayments(String customerId, Pageable pageable);

    PaymentResponse refundPayment(UUID id, String customerId, String idempotencyKey, RefundRequest request);
}
