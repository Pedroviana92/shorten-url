package com.shorten.url.shortenUrl.services.impl;

import com.shorten.url.shortenUrl.services.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public class CacheServiceImpl implements CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheServiceImpl.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final Duration defaultTtl;
    private final String keyPrefix;

    public CacheServiceImpl(
            RedisTemplate<String, Object> redisTemplate,
            @Value("${app.cache.ttl-seconds:10}") int ttlSeconds,
            @Value("${app.cache.key-prefix:urlshortener:}") String keyPrefix) {
        this.redisTemplate = redisTemplate;
        this.defaultTtl = Duration.ofSeconds(ttlSeconds);
        this.keyPrefix = keyPrefix;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            String fullKey = keyPrefix + key;
            Object value = redisTemplate.opsForValue().get(fullKey);
            if (value == null) {
                return Optional.empty();
            }
            log.debug("Cache HIT for key: {}", key);
            return Optional.of((T) value);
        } catch (Exception e) {
            log.warn("Error getting from cache: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, Object value) {
        put(key, value, defaultTtl);
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        try {
            String fullKey = keyPrefix + key;
            redisTemplate.opsForValue().set(fullKey, value, ttl);
            log.debug("Cached value for key: {} with TTL: {}s", key, ttl.getSeconds());
        } catch (Exception e) {
            log.warn("Error putting to cache: {}", e.getMessage());
        }
    }

    @Override
    public void delete(String key) {
        try {
            String fullKey = keyPrefix + key;
            redisTemplate.delete(fullKey);
            log.debug("Deleted cache key: {}", key);
        } catch (Exception e) {
            log.warn("Error deleting from cache: {}", e.getMessage());
        }
    }

    @Override
    public <T> T getOrCompute(String key, Supplier<T> operation, Class<T> type) {
        // Try to get from cache first
        Optional<T> cached = get(key, type);
        if (cached.isPresent()) {
            log.info("Idempotency: returning cached response for key: {}",
                key.substring(0, Math.min(8, key.length())));
            return cached.get();
        }

        // Cache miss - execute operation
        T result = operation.get();

        // Cache the result
        put(key, result);

        return result;
    }

    @Override
    public boolean exists(String key) {
        try {
            String fullKey = keyPrefix + key;
            Boolean exists = redisTemplate.hasKey(fullKey);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("Error checking cache existence: {}", e.getMessage());
            return false;
        }
    }
}
