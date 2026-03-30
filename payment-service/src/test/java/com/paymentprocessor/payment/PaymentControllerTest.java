package com.paymentprocessor.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.payment.api.PaymentController;
import com.paymentprocessor.payment.api.dto.CreatePaymentRequest;
import com.paymentprocessor.payment.api.dto.PaymentResponse;
import com.paymentprocessor.payment.domain.PaymentStatus;
import com.paymentprocessor.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @Test
    void createPayment_returns201_withValidRequest() throws Exception {
        PaymentResponse mockResponse = new PaymentResponse(
                UUID.randomUUID(), "cust_001", new BigDecimal("99.99"),
                "usd", PaymentStatus.PENDING, null, null, Instant.now(), Instant.now()
        );
        when(paymentService.createPayment(anyString(), anyString(), any(CreatePaymentRequest.class)))
                .thenReturn(mockResponse);

        CreatePaymentRequest request = new CreatePaymentRequest(new BigDecimal("99.99"), "usd", null);

        mockMvc.perform(post("/v1/payments")
                        .header("Idempotency-Key", "test-key-001")
                        .header("X-Customer-Id", "cust_001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value("cust_001"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createPayment_returns400_whenAmountMissing() throws Exception {
        mockMvc.perform(post("/v1/payments")
                        .header("Idempotency-Key", "test-key-001")
                        .header("X-Customer-Id", "cust_001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"usd\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPayment_returns400_whenCurrencyMissing() throws Exception {
        mockMvc.perform(post("/v1/payments")
                        .header("Idempotency-Key", "test-key-001")
                        .header("X-Customer-Id", "cust_001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":\"9.99\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPayment_returns400_whenIdempotencyKeyMissing() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest(BigDecimal.TEN, "usd", null);

        mockMvc.perform(post("/v1/payments")
                        .header("X-Customer-Id", "cust_001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
