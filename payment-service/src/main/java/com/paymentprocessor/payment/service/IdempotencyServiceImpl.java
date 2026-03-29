package com.paymentprocessor.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentprocessor.payment.domain.IdempotencyKey;
import com.paymentprocessor.payment.repository.IdempotencyKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class IdempotencyServiceImpl implements IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyServiceImpl.class);
    private static final String KEY_PREFIX = "idempotency:";
    private static final Duration TTL = Duration.ofHours(24);

    private final RedisTemplate<String, String> redisTemplate;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;

    public IdempotencyServiceImpl(RedisTemplate<String, String> redisTemplate,
                                   IdempotencyKeyRepository idempotencyKeyRepository,
                                   ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<IdempotencyResponse> get(String customerId, String idempotencyKey) {
        // Layer 1: Redis fast path
        String redisKey = buildRedisKey(customerId, idempotencyKey);
        try {
            String cached = redisTemplate.opsForValue().get(redisKey);
            if (cached != null) {
                return Optional.of(objectMapper.readValue(cached, IdempotencyResponse.class));
            }
        } catch (Exception e) {
            log.warn("Redis unavailable for idempotency check, falling back to DB: {}", e.getMessage());
        }

        // Layer 2: DB fallback (handles Redis unavailability)
        String keyHash = buildKeyHash(customerId, idempotencyKey);
        return idempotencyKeyRepository.findById(keyHash)
                .filter(ik -> ik.getExpiresAt().isAfter(Instant.now()))
                .map(ik -> new IdempotencyResponse(ik.getResponseStatus(), ik.getResponseBody()));
    }

    @Override
    public void store(String customerId, String idempotencyKey, IdempotencyResponse response) {
        String redisKey = buildRedisKey(customerId, idempotencyKey);
        String keyHash = buildKeyHash(customerId, idempotencyKey);

        try {
            String serialized = objectMapper.writeValueAsString(response);

            // SETNX in Redis — only set if absent
            Boolean set = redisTemplate.opsForValue().setIfAbsent(redisKey, serialized, TTL);
            if (Boolean.FALSE.equals(set)) {
                return; // key already exists, another request won the race
            }

            // Persist to DB as durable fallback
            IdempotencyKey entity = new IdempotencyKey();
            entity.setKeyHash(keyHash);
            entity.setCustomerId(customerId);
            entity.setResponseStatus(response.status());
            entity.setResponseBody(response.body());
            entity.setExpiresAt(Instant.now().plus(TTL));

            try {
                idempotencyKeyRepository.save(entity);
            } catch (DataIntegrityViolationException e) {
                // Concurrent request already saved — safe to ignore
                log.debug("Idempotency key already persisted by concurrent request: {}", keyHash);
            }
        } catch (Exception e) {
            log.error("Failed to store idempotency response for key {}: {}", keyHash, e.getMessage());
        }
    }

    private String buildRedisKey(String customerId, String idempotencyKey) {
        return KEY_PREFIX + customerId + ":" + buildKeyHash(customerId, idempotencyKey);
    }

    private String buildKeyHash(String customerId, String idempotencyKey) {
        return DigestUtils.md5DigestAsHex((customerId + ":" + idempotencyKey).getBytes());
    }
}
