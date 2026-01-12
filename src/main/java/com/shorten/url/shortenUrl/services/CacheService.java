package com.shorten.url.shortenUrl.services;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

public interface CacheService {

    /**
     * Get value from cache
     * @param key The cache key
     * @param type The expected type of the value
     * @return Optional containing the cached value, or empty if not found
     */
    <T> Optional<T> get(String key, Class<T> type);

    /**
     * Put value in cache with default TTL
     * @param key The cache key
     * @param value The value to cache
     */
    void put(String key, Object value);

    /**
     * Put value in cache with custom TTL
     * @param key The cache key
     * @param value The value to cache
     * @param ttl Time to live
     */
    void put(String key, Object value, Duration ttl);

    /**
     * Delete a key from cache
     * @param key The cache key to delete
     */
    void delete(String key);

    /**
     * Execute operation with caching (for idempotency)
     * Returns cached result if key exists, otherwise executes and caches
     * @param key The cache key
     * @param operation The operation to execute if cache miss
     * @param type The expected return type
     * @return The cached or newly computed value
     */
    <T> T getOrCompute(String key, Supplier<T> operation, Class<T> type);

    /**
     * Check if a key exists in cache
     * @param key The cache key
     * @return true if key exists
     */
    boolean exists(String key);
}
