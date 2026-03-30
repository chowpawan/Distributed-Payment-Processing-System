package com.paymentprocessor.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.paymentprocessor.payment.domain.IdempotencyKey;
import com.paymentprocessor.payment.repository.IdempotencyKeyRepository;
import com.paymentprocessor.payment.service.IdempotencyService.IdempotencyResponse;
import com.paymentprocessor.payment.service.IdempotencyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceImplTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    private IdempotencyServiceImpl service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        service = new IdempotencyServiceImpl(redisTemplate, idempotencyKeyRepository, objectMapper);
    }

    @Test
    void get_returnsEmpty_whenKeyNotInRedisOrDb() {
        when(valueOps.get(anyString())).thenReturn(null);
        when(idempotencyKeyRepository.findById(anyString())).thenReturn(Optional.empty());

        Optional<IdempotencyResponse> result = service.get("cust_001", "key-001");

        assertTrue(result.isEmpty());
    }

    @Test
    void get_returnsCachedResponse_whenKeyInRedis() throws Exception {
        IdempotencyResponse expected = new IdempotencyResponse(201, "{\"id\":\"pay-001\"}");
        String json = objectMapper.writeValueAsString(expected);
        when(valueOps.get(anyString())).thenReturn(json);

        Optional<IdempotencyResponse> result = service.get("cust_001", "key-001");

        assertTrue(result.isPresent());
        assertEquals(201, result.get().status());
        assertEquals("{\"id\":\"pay-001\"}", result.get().body());
    }

    @Test
    void store_callsSetnx_withCorrectTtl() throws Exception {
        IdempotencyResponse response = new IdempotencyResponse(201, "{\"id\":\"pay-001\"}");
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        service.store("cust_001", "key-001", response);

        verify(valueOps).setIfAbsent(
                argThat(k -> k.startsWith("idempotency:cust_001:")),
                anyString(),
                eq(Duration.ofHours(24))
        );
    }

    @Test
    void store_doesNotSaveToDb_whenRedisKeyAlreadyExists() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        service.store("cust_001", "key-001", new IdempotencyResponse(201, "{}"));

        verify(idempotencyKeyRepository, never()).save(any(IdempotencyKey.class));
    }

    @Test
    void differentKeys_doNotCollide() throws Exception {
        IdempotencyResponse resp1 = new IdempotencyResponse(201, "{\"id\":\"pay-001\"}");
        IdempotencyResponse resp2 = new IdempotencyResponse(201, "{\"id\":\"pay-002\"}");
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        service.store("cust_001", "key-001", resp1);
        service.store("cust_001", "key-002", resp2);

        // Two distinct Redis keys were set
        verify(valueOps, times(2)).setIfAbsent(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void sameKey_differentCustomers_doNotCollide() throws Exception {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        service.store("cust_001", "shared-key", new IdempotencyResponse(201, "{}"));
        service.store("cust_002", "shared-key", new IdempotencyResponse(201, "{}"));

        // Captures both Redis keys and verifies they differ
        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(valueOps, times(2)).setIfAbsent(captor.capture(), anyString(), any(Duration.class));
        assertNotEquals(captor.getAllValues().get(0), captor.getAllValues().get(1));
    }
}
