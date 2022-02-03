package org.uniprot.api.common.concurrency;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created 02/02/2022
 *
 * @author Edd
 */
@Slf4j
public class Gatekeeper {
    protected static final int DEFAULT_TIMEOUT = 5;
    private final Semaphore semaphore;
    private final int capacity;
    private final int timeout;

    public Gatekeeper(int capacity) {
        this.semaphore = new Semaphore(capacity);
        this.timeout = DEFAULT_TIMEOUT;
        this.capacity = capacity;
    }

    public Gatekeeper(int capacity, int entryTimeoutSeconds) {

        this.semaphore = new Semaphore(capacity);
        this.capacity = capacity;

        if (entryTimeoutSeconds > 0) {
            timeout = entryTimeoutSeconds;
        } else {
            timeout = DEFAULT_TIMEOUT;
        }
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean enter() {
        try {
            return semaphore.tryAcquire(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("Thread interrupted", e);
            Thread.currentThread().interrupt();
        }
        return true; // by default, allow entry
    }

    public void exit() {
        semaphore.release();
    }

    public int getSpaceInside() {
        return semaphore.availablePermits();
    }

    int getTimeout() {
        return timeout;
    }
}
