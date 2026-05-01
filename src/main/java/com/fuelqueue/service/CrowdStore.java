package com.fuelqueue.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory crowd store.
 * Tracks which users are currently at which stations.
 * Each entry auto-expires after ttlSeconds of inactivity (simulating Redis TTL).
 */
@Component
public class CrowdStore {

    @Value("${app.crowd.user-ttl-seconds:30}")
    private int ttlSeconds;

    // stationId -> { userId -> lastSeenTimestampMs }
    private final Map<Long, Map<Long, Long>> store = new ConcurrentHashMap<>();

    public void recordPresence(Long stationId, Long userId) {
        store.computeIfAbsent(stationId, k -> new ConcurrentHashMap<>())
             .put(userId, System.currentTimeMillis());
    }

    public int getActiveCount(Long stationId) {
        Map<Long, Long> users = store.get(stationId);
        if (users == null) return 0;
        long cutoff = System.currentTimeMillis() - (ttlSeconds * 1000L);
        // Count only users seen within the TTL window
        return (int) users.values().stream().filter(ts -> ts >= cutoff).count();
    }

    public Set<Long> getActiveUserIds(Long stationId) {
        Map<Long, Long> users = store.get(stationId);
        if (users == null) return Set.of();
        long cutoff = System.currentTimeMillis() - (ttlSeconds * 1000L);
        return users.entrySet().stream()
                    .filter(e -> e.getValue() >= cutoff)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
    }

    /** Clean up stale entries (call periodically or on demand) */
    public void evictStale() {
        long cutoff = System.currentTimeMillis() - (ttlSeconds * 1000L);
        store.values().forEach(users -> users.entrySet()
             .removeIf(e -> e.getValue() < cutoff));
    }
}
