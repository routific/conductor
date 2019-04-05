package com.netflix.conductor.core.utils;

/**
 * Locking layer which can be used as Distributed locks backed by Redis sets or any other providers.
 */
public interface LockingService {

    boolean acquire(String key);

    boolean acquire(String key, int expireDuration);

    boolean release(String key);
}
