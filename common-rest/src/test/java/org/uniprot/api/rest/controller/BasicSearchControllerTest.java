package org.uniprot.api.rest.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.USER_AGENT;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
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
        MatcherAssert.assertThat(
                gatekeeper.getSpaceInside(), CoreMatchers.is(gatekeeper.getCapacity()));
    }

    @Test
    void runIfNotBusyReleasesGatekeeperWhenContextSupplierThrowsRuntimeException() {
        setUp(true);

        RuntimeException errorResult = new RuntimeException("MOCK EXCEPTION");
        HttpServletRequest request = mock(HttpServletRequest.class);

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                controller.runRequestIfNotBusy(
                                        () -> {
                                            throw errorResult;
                                        },
                                        request,
                                        new DeferredResult<>()));

        assertEquals(errorResult, exception);
        MatcherAssert.assertThat(
                gatekeeper.getSpaceInside(), CoreMatchers.is(gatekeeper.getCapacity()));
    }

    @Test
    void creatingDeferredResultDoesNotReleaseGatekeeperWhenEnterFails()
            throws InterruptedException {
        Gatekeeper gatekeeper = new Gatekeeper(0, 1);
        ThreadPoolTaskExecutor downloadTaskExecutor = new ThreadPoolTaskExecutor();
        downloadTaskExecutor.initialize();
        setUp(gatekeeper, downloadTaskExecutor);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(USER_AGENT)).thenReturn("python-requests/2.31.0");

        DeferredResult<ResponseEntity<MessageConverterContext<String>>> result =
                controller.getDeferredResultResponseEntity(() -> context, request);

        Thread.sleep(1200);

        assertInstanceOf(TooManyRequestsException.class, result.getResult());
        MatcherAssert.assertThat(gatekeeper.getSpaceInside(), CoreMatchers.is(0));
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

    @ParameterizedTest
    @CsvSource({"true,false", "false,false", "true,true", "false,true"})
    void lateCompletionClosesStreamsAndReleasesPermit(boolean withGatekeeper, boolean closeFails) {
        AtomicReference<Runnable> task = prepareQueuedRequest(withGatekeeper);
        AtomicBoolean entitiesClosed = new AtomicBoolean();
        AtomicBoolean idsClosed = new AtomicBoolean();
        context.setEntities(
                Stream.of("entry")
                        .onClose(
                                () -> {
                                    entitiesClosed.set(true);
                                    if (closeFails) {
                                        throw new IllegalStateException("close failed");
                                    }
                                }));
        context.setEntityIds(Stream.of("id").onClose(() -> idsClosed.set(true)));
        AtomicReference<DeferredResult<ResponseEntity<MessageConverterContext<String>>>> resultRef =
                new AtomicReference<>();
        RuntimeException timeout = new RuntimeException("Request timed out during Solr open");
        var result =
                controller.getDeferredResultResponseEntity(
                        () -> {
                            // The request expires while the worker is opening its streams.
                            resultRef.get().setErrorResult(timeout);
                            return context;
                        },
                        mock(HttpServletRequest.class));
        resultRef.set(result);

        task.get().run();

        assertSame(timeout, result.getResult());
        assertTrue(entitiesClosed.get());
        assertTrue(idsClosed.get());
        if (withGatekeeper) {
            assertEquals(gatekeeper.getCapacity(), gatekeeper.getSpaceInside());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void expiredQueuedRequestDoesNotOpenStreams(boolean withGatekeeper) {
        AtomicReference<Runnable> task = prepareQueuedRequest(withGatekeeper);
        Supplier<MessageConverterContext<String>> supplier = mock(Supplier.class);
        var result =
                controller.getDeferredResultResponseEntity(
                        supplier, mock(HttpServletRequest.class));
        result.setErrorResult(new RuntimeException("Request timed out in queue"));

        task.get().run();

        Mockito.verifyNoInteractions(supplier);
        if (withGatekeeper) {
            assertEquals(gatekeeper.getCapacity(), gatekeeper.getSpaceInside());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void acceptedResponseLeavesStreamsOpenForConverter(boolean withGatekeeper) {
        AtomicReference<Runnable> task = prepareQueuedRequest(withGatekeeper);
        AtomicBoolean closed = new AtomicBoolean();
        context.setEntities(Stream.of("entry").onClose(() -> closed.set(true)));
        var result =
                controller.getDeferredResultResponseEntity(
                        () -> context, mock(HttpServletRequest.class));

        task.get().run();

        assertInstanceOf(ResponseEntity.class, result.getResult());
        assertFalse(closed.get());
        if (withGatekeeper) {
            assertEquals(gatekeeper.getCapacity() - 1, gatekeeper.getSpaceInside());
        }
        context.getEntities().close();
    }

    private AtomicReference<Runnable> prepareQueuedRequest(boolean withGatekeeper) {
        ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);
        AtomicReference<Runnable> task = new AtomicReference<>();
        Mockito.doAnswer(
                        invocation -> {
                            task.set(invocation.getArgument(0));
                            return null;
                        })
                .when(executor)
                .execute(any(Runnable.class));
        setUp(withGatekeeper ? new Gatekeeper(10) : null, executor);
        return task;
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
