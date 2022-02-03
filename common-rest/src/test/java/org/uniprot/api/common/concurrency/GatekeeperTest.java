package org.uniprot.api.common.concurrency;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class GatekeeperTest {
    @Test
    void canEnterAndExit() {
        int capacity = 10;
        Gatekeeper gatekeeper = new Gatekeeper(capacity);

        gatekeeper.enter();

        assertThat(gatekeeper.getSpaceInside(), is(capacity - 1));

        gatekeeper.exit();

        assertThat(gatekeeper.getSpaceInside(), is(capacity));
        assertThat(gatekeeper.getCapacity(), is(capacity));
    }

    @Test
    void canEnterOnThreadAndExitOnAnother() throws InterruptedException {
        int capacity = 11;
        Gatekeeper gatekeeper = new Gatekeeper(capacity);

        // enter 10 times
        ThreadPoolExecutor requestHandlers = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            requestHandlers.execute(gatekeeper::enter);
        }

        Random random = new Random();
        ThreadPoolExecutor responseHandlers = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);

        // exit 10 times
        for (int i = 0; i < 10; i++) {
            responseHandlers.execute(
                    () -> {
                        int timeToWriteOutput = random.nextInt(100);
                        try {
                            Thread.sleep(timeToWriteOutput);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        gatekeeper.exit();
                    });
        }

        requestHandlers.awaitTermination(2, TimeUnit.SECONDS);
        responseHandlers.awaitTermination(2, TimeUnit.SECONDS);
        requestHandlers.shutdown();
        responseHandlers.shutdown();

        assertThat(gatekeeper.getCapacity(), is(capacity));
    }

    @Test
    void cannotEnterWhenFull() {
        Gatekeeper gatekeeper = new Gatekeeper(2, 1);
        
        gatekeeper.enter();
        gatekeeper.enter();

        assertThat(gatekeeper.enter(), is(false));
    }

    @Test
    void checkDefaultTimeoutIsUsedForNegativeTimeout() {
        Gatekeeper gatekeeper = new Gatekeeper(1, -1);

        assertThat(gatekeeper.getTimeout(), is(Gatekeeper.DEFAULT_TIMEOUT));
    }
}
