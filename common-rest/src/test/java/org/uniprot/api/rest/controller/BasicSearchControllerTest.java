package org.uniprot.api.rest.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.USER_AGENT;

import javax.servlet.http.HttpServletRequest;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.common.exception.ImportantMessageServiceException;
import org.uniprot.api.rest.app.FakeBasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;

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
                FakeBasicSearchController.createInstance(
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
        controller.getDeferredResultResponseEntity(() -> context, mock(HttpServletRequest.class));
        Thread.sleep(100); // give some time to allow the Gatekeeper to allow an "enter"

        MatcherAssert.assertThat(
                gatekeeper.getSpaceInside(), CoreMatchers.is(gatekeeper.getCapacity() - 1));

        // SECOND STREAM REQUEST
        controller.getDeferredResultResponseEntity(() -> context, mock(HttpServletRequest.class));
        Thread.sleep(100); // give some time to allow the Gatekeeper to allow an "enter"

        MatcherAssert.assertThat(
                gatekeeper.getSpaceInside(), CoreMatchers.is(gatekeeper.getCapacity() - 2));
    }

    @Test
    void noErrorWhenCreatingDeferredResultIfGatekeeperNull() throws InterruptedException {
        setUp(false);

        controller.getDeferredResultResponseEntity(() -> context, mock(HttpServletRequest.class));

        assertTrue(true);
    }

    @Test
    void invalidMediaTypeThrowsException() throws InterruptedException {
        setUp(false);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(HttpHeaders.ACCEPT)).thenReturn("INVALID");
        assertThrows(
                ImportantMessageServiceException.class, () -> controller.getAcceptHeader(request));
    }

    @Test
    void browserRequestsBypassGatekeeper() throws Exception {
        setUp(true); // set with gatekeeper
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getHeader(USER_AGENT))
                .thenReturn(
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.54 Safari/537.36");
        for (int i = 0; i < 5; i++) {
            controller.getDeferredResultResponseEntity(() -> context, httpRequest);
            Thread.sleep(100); // give some time to allow the Gatekeeper to allow an "enter"
            // gatekeeper capacity remains unchanged because the browser request bypasses it
            MatcherAssert.assertThat(
                    gatekeeper.getSpaceInside(), CoreMatchers.is(gatekeeper.getCapacity()));
        }
    }

    @Test
    void nonBrowserRequestsGoThroughGatekeeper() throws Exception {
        setUp(true); // set with gatekeeper
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getHeader(USER_AGENT)).thenReturn("python-requests/2.31.0");
        controller.getDeferredResultResponseEntity(() -> context, httpRequest);
        Thread.sleep(100); // give some time to allow the Gatekeeper to allow an "enter"
        MatcherAssert.assertThat(
                gatekeeper.getSpaceInside(), CoreMatchers.is(gatekeeper.getCapacity() - 1));
    }
}
