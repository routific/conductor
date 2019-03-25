package com.netflix.conductor.core.utils;

/**
 * Locking layer which can be used as Distributed locks backed by Redis sets or any other providers.
 */
public interface LockingService {

    boolean acquire(String key);

    boolean release(String key);
}
