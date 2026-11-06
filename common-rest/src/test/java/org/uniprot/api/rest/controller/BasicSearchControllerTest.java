package org.uniprot.api.rest.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.USER_AGENT;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.StandardServletAsyncWebRequest;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
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
    private static final String BROWSER_USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.54 Safari/537.36";

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
    void runIfNotBusyClosesStreamAndExitsGatekeeperWhenRequestAlreadyCompleted() {
        setUp(true);
        AtomicBoolean streamClosed = new AtomicBoolean(false);
        context.setEntities(Stream.of("a", "b").onClose(() -> streamClosed.set(true)));
        DeferredResult<ResponseEntity<MessageConverterContext<String>>> deferredResult =
                new DeferredResult<>();
        // simulate the async request having already completed, e.g. the client disconnected
        deferredResult.setErrorResult(new RuntimeException("client went away"));

        controller.runRequestIfNotBusy(
                () -> context, mock(HttpServletRequest.class), deferredResult);

        assertTrue(streamClosed.get());
        MatcherAssert.assertThat(
                gatekeeper.getSpaceInside(), CoreMatchers.is(gatekeeper.getCapacity()));
    }

    @Test
    void runIfNotBusyDoesNotCloseStreamWhenResultAccepted() {
        setUp(true);
        AtomicBoolean streamClosed = new AtomicBoolean(false);
        context.setEntities(Stream.of("a", "b").onClose(() -> streamClosed.set(true)));

        controller.runRequestIfNotBusy(
                () -> context, mock(HttpServletRequest.class), new DeferredResult<>());

        // the message converter is now responsible for closing the stream and exiting
        assertFalse(streamClosed.get());
        MatcherAssert.assertThat(
                gatekeeper.getSpaceInside(), CoreMatchers.is(gatekeeper.getCapacity() - 1));
    }

    @Test
    void runIfNotBusyClosesStreamAndExitsGatekeeperWhenBuildingResponseFails() {
        setUp(true);
        AtomicBoolean streamClosed = new AtomicBoolean(false);
        context.setEntities(Stream.of("a", "b").onClose(() -> streamClosed.set(true)));
        context.setContentType(MediaType.APPLICATION_JSON);
        context.setDownloadContentDispositionHeader(true);
        HttpServletRequest request = mock(HttpServletRequest.class);
        // fails while building the download headers, i.e. after the results stream was opened
        RuntimeException errorResult = new RuntimeException("MOCK EXCEPTION");
        when(request.getParameter(anyString())).thenThrow(errorResult);

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                controller.runRequestIfNotBusy(
                                        () -> context, request, new DeferredResult<>()));

        assertEquals(errorResult, exception);
        assertTrue(streamClosed.get());
        MatcherAssert.assertThat(
                gatekeeper.getSpaceInside(), CoreMatchers.is(gatekeeper.getCapacity()));
    }

    @Test
    void browserRequestClosesStreamWhenRequestCompletedBeforeResultsReady() throws Exception {
        setUp(true);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getHeader(USER_AGENT)).thenReturn(BROWSER_USER_AGENT);
        CountDownLatch requestCompleted = new CountDownLatch(1);
        CountDownLatch streamClosed = new CountDownLatch(1);
        context.setEntities(Stream.of("a", "b").onClose(streamClosed::countDown));

        DeferredResult<ResponseEntity<MessageConverterContext<String>>> result =
                controller.getDeferredResultResponseEntity(
                        () -> {
                            // simulates a slow Solr stream open
                            awaitQuietly(requestCompleted);
                            return context;
                        },
                        httpRequest);
        // the client goes away while the results stream is still being opened
        result.setErrorResult(new RuntimeException("client went away"));
        requestCompleted.countDown();

        assertTrue(streamClosed.await(5, TimeUnit.SECONDS));
    }

    @Test
    void streamClosedWhenRequestCompletesWithoutResultsBeingWritten() throws Exception {
        setUp(true);
        AtomicBoolean streamClosed = new AtomicBoolean(false);
        context.setEntities(Stream.of("a", "b").onClose(() -> streamClosed.set(true)));
        MockHttpServletRequest servletRequest = asyncBrowserRequest();
        WebAsyncManager asyncManager = asyncManager(servletRequest);

        DeferredResult<ResponseEntity<MessageConverterContext<String>>> result =
                controller.getDeferredResultResponseEntity(() -> context, servletRequest);
        asyncManager.startDeferredResultProcessing(result);
        awaitResult(result);
        // the result was accepted, so closing the stream is left to the message converter
        assertFalse(streamClosed.get());

        // the async request completes, but the message converter never wrote the results
        servletRequest.getAsyncContext().complete();

        assertTrue(streamClosed.get());
    }

    @Test
    void safetyNetClosesStreamAgainAfterConverterClosedItWithoutAffectingResponse()
            throws Exception {
        setUp(true);
        // a mock (rather than a real Stream, whose close actions the JDK only ever runs once)
        // so that every close() call made on the context's stream is counted
        @SuppressWarnings("unchecked")
        Stream<String> entities = mock(Stream.class);
        context.setEntities(entities);
        MockHttpServletRequest servletRequest = asyncBrowserRequest();
        WebAsyncManager asyncManager = asyncManager(servletRequest);

        DeferredResult<ResponseEntity<MessageConverterContext<String>>> result =
                controller.getDeferredResultResponseEntity(() -> context, servletRequest);
        asyncManager.startDeferredResultProcessing(result);
        awaitResult(result);
        entities.close(); // what the message converter does after writing the results
        servletRequest.getAsyncContext().complete();

        // the safety net closes the same stream again: this relies on close() being idempotent
        verify(entities, times(2)).close();
        ResponseEntity<?> response = (ResponseEntity<?>) result.getResult();
        assertSame(context, response.getBody());
    }

    @Test
    void runIfNotBusyClosesEntityIdsStreamWhenRequestAlreadyCompleted() {
        setUp(true);
        AtomicBoolean idsClosed = new AtomicBoolean(false);
        context.setEntityIds(Stream.of("id1", "id2").onClose(() -> idsClosed.set(true)));
        DeferredResult<ResponseEntity<MessageConverterContext<String>>> deferredResult =
                new DeferredResult<>();
        deferredResult.setErrorResult(new RuntimeException("client went away"));

        controller.runRequestIfNotBusy(
                () -> context, mock(HttpServletRequest.class), deferredResult);

        assertTrue(idsClosed.get());
        MatcherAssert.assertThat(
                gatekeeper.getSpaceInside(), CoreMatchers.is(gatekeeper.getCapacity()));
    }

    @Test
    void entityIdsStreamClosedWhenRequestCompletesWithoutResultsBeingWritten() throws Exception {
        setUp(true);
        AtomicBoolean idsClosed = new AtomicBoolean(false);
        context.setEntityIds(Stream.of("id1", "id2").onClose(() -> idsClosed.set(true)));
        MockHttpServletRequest servletRequest = asyncBrowserRequest();
        WebAsyncManager asyncManager = asyncManager(servletRequest);

        DeferredResult<ResponseEntity<MessageConverterContext<String>>> result =
                controller.getDeferredResultResponseEntity(() -> context, servletRequest);
        asyncManager.startDeferredResultProcessing(result);
        awaitResult(result);
        assertFalse(idsClosed.get());

        servletRequest.getAsyncContext().complete();

        assertTrue(idsClosed.get());
    }

    @Test
    void browserRequestClosesStreamWhenBuildingResponseFails() throws Exception {
        setUp(true);
        CountDownLatch streamClosed = new CountDownLatch(1);
        context.setEntities(Stream.of("a", "b").onClose(streamClosed::countDown));
        context.setContentType(MediaType.APPLICATION_JSON);
        context.setDownloadContentDispositionHeader(true);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getHeader(USER_AGENT)).thenReturn(BROWSER_USER_AGENT);
        RuntimeException errorResult = new RuntimeException("MOCK EXCEPTION");
        // fails while building the download headers, i.e. after the results stream was opened
        when(httpRequest.getParameter(anyString())).thenThrow(errorResult);

        // no async manager, so the onCompletion safety net cannot be what closes the stream
        DeferredResult<ResponseEntity<MessageConverterContext<String>>> result =
                controller.getDeferredResultResponseEntity(() -> context, httpRequest);

        assertTrue(streamClosed.await(5, TimeUnit.SECONDS));
        awaitResult(result);
        assertSame(errorResult, result.getResult());
    }

    @Test
    void runIfNotBusyExitsGatekeeperAndClosesEntityIdsWhenClosingEntitiesFails() {
        setUp(true);
        AtomicBoolean idsClosed = new AtomicBoolean(false);
        context.setEntities(throwingOnCloseStream());
        context.setEntityIds(Stream.of("id1").onClose(() -> idsClosed.set(true)));
        DeferredResult<ResponseEntity<MessageConverterContext<String>>> deferredResult =
                new DeferredResult<>();
        deferredResult.setErrorResult(new RuntimeException("client went away"));

        assertDoesNotThrow(
                () ->
                        controller.runRequestIfNotBusy(
                                () -> context, mock(HttpServletRequest.class), deferredResult));

        assertTrue(idsClosed.get());
        MatcherAssert.assertThat(
                gatekeeper.getSpaceInside(), CoreMatchers.is(gatekeeper.getCapacity()));
    }

    @Test
    void runIfNotBusyRethrowsOriginalErrorWhenBuildingResponseAndClosingEntitiesBothFail() {
        setUp(true);
        AtomicBoolean idsClosed = new AtomicBoolean(false);
        context.setEntities(throwingOnCloseStream());
        context.setEntityIds(Stream.of("id1").onClose(() -> idsClosed.set(true)));
        context.setContentType(MediaType.APPLICATION_JSON);
        context.setDownloadContentDispositionHeader(true);
        HttpServletRequest request = mock(HttpServletRequest.class);
        RuntimeException errorResult = new RuntimeException("MOCK EXCEPTION");
        when(request.getParameter(anyString())).thenThrow(errorResult);

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                controller.runRequestIfNotBusy(
                                        () -> context, request, new DeferredResult<>()));

        assertSame(errorResult, exception);
        assertTrue(idsClosed.get());
        MatcherAssert.assertThat(
                gatekeeper.getSpaceInside(), CoreMatchers.is(gatekeeper.getCapacity()));
    }

    /** Mimics StoreStreamer/TupleStreamDocumentIdStream when closing the TupleStream fails. */
    private static Stream<String> throwingOnCloseStream() {
        return Stream.of("a", "b")
                .onClose(
                        () -> {
                            throw new IllegalStateException("Error when closing TupleStream");
                        });
    }

    private static MockHttpServletRequest asyncBrowserRequest() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader(USER_AGENT, BROWSER_USER_AGENT);
        servletRequest.setAsyncSupported(true);
        return servletRequest;
    }

    private static WebAsyncManager asyncManager(MockHttpServletRequest servletRequest) {
        WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(servletRequest);
        asyncManager.setAsyncWebRequest(
                new StandardServletAsyncWebRequest(servletRequest, new MockHttpServletResponse()));
        return asyncManager;
    }

    private static void awaitResult(DeferredResult<?> result) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (!result.hasResult() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        assertTrue(result.hasResult(), "Timed out waiting for the deferred result to be set");
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
