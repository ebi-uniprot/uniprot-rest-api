package org.uniprot.api.rest.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.USER_AGENT;

import javax.servlet.http.HttpServletRequest;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.common.exception.ImportantMessageServiceException;
import org.uniprot.api.common.exception.TooManyRequestsException;
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
        Gatekeeper gatekeeper = null;
        if (withGatekeeper) {
            gatekeeper = new Gatekeeper(10);
        }
        ThreadPoolTaskExecutor downloadTaskExecutor = new ThreadPoolTaskExecutor();
        downloadTaskExecutor.initialize();
        setUp(gatekeeper, downloadTaskExecutor);
    }

    void setUp(Gatekeeper gatekeeper, ThreadPoolTaskExecutor downloadTaskExecutor) {
        this.gatekeeper = gatekeeper;
        MessageConverterContextFactory<String> contextFactory =
                new MessageConverterContextFactory<>();
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
    void creatingDeferredResultWhenThrowsRuntimeException() throws InterruptedException {
        setUp(true);

        RuntimeException errorResult = new RuntimeException("MOCK EXCEPTION");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(anyString())).thenThrow(errorResult);
        DeferredResult<ResponseEntity<MessageConverterContext<String>>> result =
                controller.getDeferredResultResponseEntity(() -> context, request);
        assertNotNull(result);
        Thread.sleep(200);
        assertInstanceOf(RuntimeException.class, result.getResult());
        RuntimeException exception = (RuntimeException) result.getResult();
        assertEquals(errorResult, exception);
    }

    @Test
    void creatingDeferredResultWhenThreadExecutorThrowsTaskRejectedException() {
        ThreadPoolTaskExecutor downloadTaskExecutor = Mockito.mock(ThreadPoolTaskExecutor.class);
        Gatekeeper gatekeeper = new Gatekeeper(10);
        setUp(gatekeeper, downloadTaskExecutor);

        TaskRejectedException errorResult = new TaskRejectedException("MOCK EXCEPTION");

        Mockito.doThrow(errorResult).when(downloadTaskExecutor).execute(any());

        HttpServletRequest request = mock(HttpServletRequest.class);

        TooManyRequestsException resultError =
                assertThrows(
                        TooManyRequestsException.class,
                        () -> controller.getDeferredResultResponseEntity(() -> context, request));
        assertNotNull(resultError);
        assertEquals(
                "Task executor rejected stream request (space inside=10)",
                resultError.getMessage());
    }

    @Test
    void runIfNotBusyNotEnterGatekeeperThrowsTooManyRequestsException() {
        Gatekeeper gatekeeper = Mockito.mock(Gatekeeper.class);
        Mockito.when(gatekeeper.enter()).thenReturn(false);
        ThreadPoolTaskExecutor downloadTaskExecutor = new ThreadPoolTaskExecutor();
        downloadTaskExecutor.initialize();
        setUp(gatekeeper, downloadTaskExecutor);

        HttpServletRequest request = mock(HttpServletRequest.class);
        TooManyRequestsException exception =
                assertThrows(
                        TooManyRequestsException.class,
                        () ->
                                controller.runRequestIfNotBusy(
                                        () -> context, request, new DeferredResult<>()));
        assertEquals("Gatekeeper did NOT let me in (space inside=0)", exception.getMessage());
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
