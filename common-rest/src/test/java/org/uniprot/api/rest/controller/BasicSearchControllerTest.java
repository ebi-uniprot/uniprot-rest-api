package org.uniprot.api.rest.controller;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.rest.app.FakeBasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;

import javax.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * The purpose of this test class is to ensure the {@link BasicSearchController} interacts correctly
 * with the {@link Gatekeeper}.
 */
class BasicSearchControllerTest {

    private MessageConverterContext<String> context;
    private Gatekeeper gatekeeper;
    private FakeBasicSearchController controller;

    void setUp(boolean withGatekeeper) {
        if (withGatekeeper) {
            gatekeeper = new Gatekeeper(10);
        } else {
            gatekeeper = null;
        }

        MessageConverterContextFactory<String> contextFactory =
                new MessageConverterContextFactory<>();
        ThreadPoolTaskExecutor downloadTaskExecutor = new ThreadPoolTaskExecutor();
        downloadTaskExecutor.initialize();
        controller =
                new FakeBasicSearchController(
                        new GenericWebApplicationContext(),
                        contextFactory,
                        downloadTaskExecutor,
                        gatekeeper);

        context = MessageConverterContext.<String>builder().build();
    }

    @Test
    void creatingDeferredResultEnsuresGatekeeperEntry() throws InterruptedException {
        setUp(true);

        // FIRST STREAM REQUEST
        controller.getDeferredResultResponseEntity(mock(HttpServletRequest.class), context);
        Thread.sleep(100); // give some time to allow the Gatekeeper to allow an "enter"

        MatcherAssert.assertThat(
                gatekeeper.getSpaceInside(), CoreMatchers.is(gatekeeper.getCapacity() - 1));

        // SECOND STREAM REQUEST
        controller.getDeferredResultResponseEntity(mock(HttpServletRequest.class), context);
        Thread.sleep(100); // give some time to allow the Gatekeeper to allow an "enter"

        MatcherAssert.assertThat(
                gatekeeper.getSpaceInside(), CoreMatchers.is(gatekeeper.getCapacity() - 2));
    }

    @Test
    void noErrorWhenCreatingDeferredResultIfGatekeeperNull() throws InterruptedException {
        setUp(false);

        controller.getDeferredResultResponseEntity(mock(HttpServletRequest.class), context);

        assertTrue(true);
    }
}
