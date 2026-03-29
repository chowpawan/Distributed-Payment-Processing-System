package com.paymentprocessor.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.events.PaymentInitiated;
import com.paymentprocessor.payment.api.dto.CreatePaymentRequest;
import com.paymentprocessor.payment.api.dto.PagedPaymentResponse;
import com.paymentprocessor.payment.api.dto.PaymentResponse;
import com.paymentprocessor.payment.api.dto.RefundRequest;
import com.paymentprocessor.payment.domain.Payment;
import com.paymentprocessor.payment.domain.PaymentStatus;
import com.paymentprocessor.payment.kafka.PaymentEventPublisher;
import com.paymentprocessor.payment.metrics.PaymentMetrics;
import com.paymentprocessor.payment.repository.PaymentRepository;
import com.paymentprocessor.payment.service.IdempotencyService.IdempotencyResponse;
import com.paymentprocessor.payment.stripe.StripeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final IdempotencyService idempotencyService;
    private final PaymentEventPublisher eventPublisher;
    private final StripeClient stripeClient;
    private final PaymentMetrics metrics;
    private final ObjectMapper objectMapper;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                               IdempotencyService idempotencyService,
                               PaymentEventPublisher eventPublisher,
                               StripeClient stripeClient,
                               PaymentMetrics metrics,
                               ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.idempotencyService = idempotencyService;
        this.eventPublisher = eventPublisher;
        this.stripeClient = stripeClient;
        this.metrics = metrics;
        this.objectMapper = objectMapper;
    }

    @Override
    public PaymentResponse createPayment(String customerId, String idempotencyKey,
                                          CreatePaymentRequest request) {
        // Check idempotency cache — return cached response if this key was seen before
        var cached = idempotencyService.get(customerId, idempotencyKey);
        if (cached.isPresent()) {
            log.debug("Idempotency hit for customer={} key={}", customerId, idempotencyKey);
            metrics.idempotencyHit();
            try {
                return objectMapper.readValue(cached.get().body(), PaymentResponse.class);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to deserialize cached response");
            }
        }

        // Create payment record in PENDING state
        Payment payment = new Payment();
        payment.setCustomerId(customerId);
        payment.setAmount(request.amount());
        payment.setCurrency(request.currency().toLowerCase());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setMetadata(request.metadata());
        payment = paymentRepository.save(payment);

        metrics.paymentCreated();
        log.info("Payment created: id={} customerId={} amount={}", payment.getId(), customerId, request.amount());

        // Publish PaymentInitiated — saga-orchestrator picks this up and charges Stripe.
        // Known trade-off: if Kafka is unavailable after the DB save, the event is lost.
        // recon-worker will mark this FAILED after 30 minutes with no Stripe record.
        String correlationId = UUID.randomUUID().toString();
        PaymentInitiated event = new PaymentInitiated(
                payment.getId().toString(),
                customerId,
                request.amount(),
                request.currency().toLowerCase(),
                idempotencyKey,
                Instant.now(),
                correlationId
        );
        eventPublisher.publish(event);

        PaymentResponse response = PaymentResponse.from(payment);

        // Cache the response for future duplicate requests
        try {
            idempotencyService.store(customerId, idempotencyKey,
                    new IdempotencyResponse(201, objectMapper.writeValueAsString(response)));
        } catch (Exception e) {
            log.warn("Failed to cache idempotency response: {}", e.getMessage());
        }

        return response;
    }

    @Override
    public PaymentResponse getPayment(UUID id) {
        return paymentRepository.findById(id)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Payment not found: " + id));
    }

    @Override
    public PagedPaymentResponse listPayments(String customerId, Pageable pageable) {
        Page<Payment> page = paymentRepository.findByCustomerId(customerId, pageable);
        List<PaymentResponse> payments = page.getContent().stream()
                .map(PaymentResponse::from)
                .toList();
        return new PagedPaymentResponse(payments, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Override
    @Transactional
    public PaymentResponse refundPayment(UUID id, String customerId, String idempotencyKey,
                                          RefundRequest request) {
        // Check idempotency for refund
        var cached = idempotencyService.get(customerId, idempotencyKey);
        if (cached.isPresent()) {
            try {
                return objectMapper.readValue(cached.get().body(), PaymentResponse.class);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to deserialize cached response");
            }
        }

        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Payment not found: " + id));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Cannot refund payment in status: " + payment.getStatus());
        }

        String stripeRefundId = stripeClient.createRefund(payment.getStripeChargeId(),
                request.amount());

        payment.setStatus(PaymentStatus.REFUNDED);
        payment = paymentRepository.save(payment);

        log.info("Payment refunded: id={} stripeRefundId={}", id, stripeRefundId);
        metrics.paymentRefunded();

        PaymentResponse response = PaymentResponse.from(payment);
        try {
            idempotencyService.store(customerId, idempotencyKey,
                    new IdempotencyResponse(200, objectMapper.writeValueAsString(response)));
        } catch (Exception e) {
            log.warn("Failed to cache idempotency response for refund: {}", e.getMessage());
        }

        return response;
    }
}
