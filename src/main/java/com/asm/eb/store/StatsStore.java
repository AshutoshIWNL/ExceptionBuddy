package com.asm.eb.store;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe store for tracking exception counts.
 * Uses AtomicLong for efficient updates in a multithreaded environment.
 *
 * @author asmishra
 * @since 2/14/2025
 */
public class StatsStore {
    private static final AtomicLong totalExceptionCount = new AtomicLong(0);
    private static final Map<String, AtomicLong> criticalExceptionStats = new ConcurrentHashMap<>();

    public static void incrementExceptionCount() {
        totalExceptionCount.incrementAndGet();
    }

    public static long getExceptionCount() {
        return totalExceptionCount.get();
    }

    public static void incrementCriticalExceptionCount(String exceptionType) {
        criticalExceptionStats.computeIfAbsent(exceptionType, key -> new AtomicLong(0)).incrementAndGet();
    }

    public static Map<String, Long> getCriticalExceptionStats() {
        Map<String, Long> snapshot = new ConcurrentHashMap<>();
        for (Map.Entry<String, AtomicLong> entry : criticalExceptionStats.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().get());
        }
        return snapshot;
    }
}
