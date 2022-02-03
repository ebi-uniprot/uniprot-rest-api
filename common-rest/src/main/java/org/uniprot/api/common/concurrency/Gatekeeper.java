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
    private final int timeout;

    public Gatekeeper(int capacity) {
        semaphore = new Semaphore(capacity);
        timeout = DEFAULT_TIMEOUT;
    }

    public Gatekeeper(int capacity, int acceptableWaitTimeForEntry) {
        semaphore = new Semaphore(capacity);

        if (acceptableWaitTimeForEntry > 0) {
            timeout = acceptableWaitTimeForEntry;
        } else {
            timeout = DEFAULT_TIMEOUT;
        }
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
}
