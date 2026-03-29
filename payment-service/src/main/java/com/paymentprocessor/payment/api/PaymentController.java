package com.paymentprocessor.payment.api;

import com.paymentprocessor.payment.api.dto.CreatePaymentRequest;
import com.paymentprocessor.payment.api.dto.PagedPaymentResponse;
import com.paymentprocessor.payment.api.dto.PaymentResponse;
import com.paymentprocessor.payment.api.dto.RefundRequest;
import com.paymentprocessor.payment.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/payments")
@Validated
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @RequestHeader("X-Customer-Id") @NotBlank String customerId,
            @Valid @RequestBody CreatePaymentRequest request) {

        PaymentResponse response = paymentService.createPayment(customerId, idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.getPayment(id));
    }

    @GetMapping
    public ResponseEntity<PagedPaymentResponse> listPayments(
            @RequestParam String customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(paymentService.listPayments(customerId, pageable));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<PaymentResponse> refundPayment(
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @RequestHeader("X-Customer-Id") @NotBlank String customerId,
            @Valid @RequestBody RefundRequest request) {

        return ResponseEntity.ok(paymentService.refundPayment(id, customerId, idempotencyKey, request));
    }
}
