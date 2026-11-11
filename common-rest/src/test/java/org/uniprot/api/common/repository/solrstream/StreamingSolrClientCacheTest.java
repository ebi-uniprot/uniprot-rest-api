package org.uniprot.api.common.repository.solrstream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.impl.HttpListenerFactory;
import org.apache.solr.client.solrj.impl.InputStreamResponseParser;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.eclipse.jetty.client.api.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import com.sun.net.httpserver.HttpServer;

class StreamingSolrClientCacheTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void streamingRequestIsAbortedWhenWaitingForHeadersTimesOut(boolean connectionOccupied)
            throws Exception {
        assertHeaderTimeoutCleanup(connectionOccupied, true);
    }

    @Test
    void rawSolrJHeaderTimeoutStillRequiresWorkaround() throws Exception {
        assertHeaderTimeoutCleanup(true, false);
    }

    private void assertHeaderTimeoutCleanup(boolean connectionOccupied, boolean guarded)
            throws Exception {
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch releaseHeaders = new CountDownLatch(1);
        AtomicInteger receivedRequests = new AtomicInteger();
        AtomicReference<Request> timedOutRequest = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        ExecutorService serverExecutor = Executors.newCachedThreadPool();
        server.setExecutor(serverExecutor);
        server.createContext(
                "/solr/hold",
                exchange -> {
                    try {
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.sendResponseHeaders(200, 0);
                        exchange.getResponseBody().write(new byte[65536]);
                        exchange.getResponseBody().flush();
                        releaseFirst.await(15, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        exchange.close();
                    }
                });
        server.createContext(
                "/solr/after",
                exchange -> {
                    receivedRequests.incrementAndGet();
                    try {
                        releaseHeaders.await(15, TimeUnit.SECONDS);
                        byte[] bytes = "{}".getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.sendResponseHeaders(200, bytes.length);
                        exchange.getResponseBody().write(bytes);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        exchange.close();
                    }
                });
        server.start();
        HttpListenerFactory observer =
                () ->
                        new HttpListenerFactory.RequestResponseListener() {
                            @Override
                            public void onQueued(Request request) {
                                // Isolate the caller's header wait from Jetty's socket timeout.
                                request.idleTimeout(10, TimeUnit.SECONDS);
                                if (!request.getPath().endsWith("/hold")) {
                                    timedOutRequest.set(request);
                                }
                            }
                        };
        try (Http2SolrClient transport =
                new Http2SolrClient.Builder(
                                "http://127.0.0.1:" + server.getAddress().getPort() + "/solr")
                        .useHttp1_1(true)
                        .withMaxConnectionsPerHost(1)
                        .withIdleTimeout(1, TimeUnit.SECONDS)
                        .withRequestTimeout(30, TimeUnit.SECONDS)
                        .withListenerFactories(List.of(observer))
                        .build()) {
            SolrClient client =
                    guarded ? StreamingSolrClientCache.abortOnHeaderTimeout(transport) : transport;
            QueryRequest hold = new QueryRequest(new ModifiableSolrParams());
            hold.setPath("/hold");
            hold.setResponseParser(new InputStreamResponseParser("json"));
            QueryRequest queued = new QueryRequest(new ModifiableSolrParams());
            queued.setPath("/after");
            queued.setResponseParser(new NoOpResponseParser("json"));
            try (InputStream first =
                    connectionOccupied
                            ? (InputStream) client.request(hold).get("stream")
                            : InputStream.nullInputStream()) {
                SolrServerException failure =
                        assertThrows(SolrServerException.class, () -> client.request(queued));
                assertInstanceOf(TimeoutException.class, failure.getCause());
                assertNotNull(timedOutRequest.get());
                if (guarded) {
                    assertSame(
                            failure.getCause(),
                            timedOutRequest.get().getAbortCause(),
                            "A caller timeout must abort the HTTP request");
                } else {
                    assertNull(
                            timedOutRequest.get().getAbortCause(),
                            "SolrJ now aborts header timeouts: review/remove StreamingSolrClientCache");
                    // Clean up the deliberately unguarded request after checking upstream behavior.
                    timedOutRequest.get().abort(failure.getCause());
                }
                if (connectionOccupied) {
                    assertEquals(
                            0,
                            first.read(),
                            "Aborting the queued request must not close another response");
                }
            }
            releaseHeaders.countDown();
            assertNotNull(
                    client.request(queued),
                    "The connection pool must recover after closing the first response");
            assertEquals(
                    connectionOccupied ? 1 : 2,
                    receivedRequests.get(),
                    "A timed-out queued request must never be sent later");
        } finally {
            releaseFirst.countDown();
            releaseHeaders.countDown();
            server.stop(0);
            serverExecutor.shutdownNow();
        }
    }

    @Test
    void headerTimeoutDoesNotRetainARequestBetweenCalls() throws Exception {
        Http2SolrClient transport = mock(Http2SolrClient.class);
        SolrClient client = StreamingSolrClientCache.abortOnHeaderTimeout(transport);
        ArgumentCaptor<HttpListenerFactory> factory =
                ArgumentCaptor.forClass(HttpListenerFactory.class);
        verify(transport).addListenerFactory(factory.capture());
        Request httpRequest = mock(Request.class);
        QueryRequest query = new QueryRequest(new ModifiableSolrParams());
        TimeoutException timeout = new TimeoutException("No response headers");
        SolrServerException failure = new SolrServerException(timeout);
        when(transport.request(query, null))
                .thenAnswer(
                        invocation -> {
                            factory.getValue().get().onQueued(httpRequest);
                            throw failure;
                        });
        assertSame(failure, assertThrows(SolrServerException.class, () -> client.request(query)));
        verify(httpRequest).abort(timeout);

        // A subsequent failure before queuing must not cancel the previous HTTP request again.
        doThrow(failure).when(transport).request(query, null);
        assertSame(failure, assertThrows(SolrServerException.class, () -> client.request(query)));
        verifyNoMoreInteractions(httpRequest);
    }

    @Test
    void unrelatedServerErrorsArePreservedWithoutExtraCancellation() throws Exception {
        Http2SolrClient transport = mock(Http2SolrClient.class);
        SolrClient client = StreamingSolrClientCache.abortOnHeaderTimeout(transport);
        ArgumentCaptor<HttpListenerFactory> factory =
                ArgumentCaptor.forClass(HttpListenerFactory.class);
        verify(transport).addListenerFactory(factory.capture());
        Request httpRequest = mock(Request.class);
        QueryRequest query = new QueryRequest(new ModifiableSolrParams());
        SolrServerException failure = new SolrServerException("Server error");
        when(transport.request(query, null))
                .thenAnswer(
                        invocation -> {
                            factory.getValue().get().onQueued(httpRequest);
                            throw failure;
                        });
        assertSame(failure, assertThrows(SolrServerException.class, () -> client.request(query)));
        verifyNoInteractions(httpRequest);
    }

    @Test
    void reusesStreamingClientWrapperForTheSameBaseUrl() throws Exception {
        StreamingSolrClientCache cache = new StreamingSolrClientCache();
        try {
            SolrClient client = cache.getHttpSolrClient("http://localhost:8983/solr");
            assertSame(client, cache.getHttpSolrClient("http://localhost:8983/solr"));
        } finally {
            cache.close();
        }
    }

    @Test
    void concurrentTimeoutsAbortOnlyTheirOwnHttpRequests() throws Exception {
        Http2SolrClient transport = mock(Http2SolrClient.class);
        SolrClient client = StreamingSolrClientCache.abortOnHeaderTimeout(transport);
        ArgumentCaptor<HttpListenerFactory> factory =
                ArgumentCaptor.forClass(HttpListenerFactory.class);
        verify(transport).addListenerFactory(factory.capture());
        QueryRequest first = new QueryRequest(new ModifiableSolrParams());
        QueryRequest second = new QueryRequest(new ModifiableSolrParams());
        Request firstHttp = mock(Request.class);
        Request secondHttp = mock(Request.class);
        TimeoutException firstTimeout = new TimeoutException("First request");
        TimeoutException secondTimeout = new TimeoutException("Second request");
        CountDownLatch bothQueued = new CountDownLatch(2);
        when(transport.request(any(), isNull()))
                .thenAnswer(
                        invocation -> {
                            boolean isFirst = invocation.getArgument(0) == first;
                            factory.getValue().get().onQueued(isFirst ? firstHttp : secondHttp);
                            bothQueued.countDown();
                            assertTrue(bothQueued.await(5, TimeUnit.SECONDS));
                            throw new SolrServerException(isFirst ? firstTimeout : secondTimeout);
                        });
        ExecutorService callers = Executors.newFixedThreadPool(2);
        try {
            var firstResult =
                    callers.submit(
                            () ->
                                    assertThrows(
                                            SolrServerException.class,
                                            () -> client.request(first)));
            var secondResult =
                    callers.submit(
                            () ->
                                    assertThrows(
                                            SolrServerException.class,
                                            () -> client.request(second)));
            assertSame(firstTimeout, firstResult.get(5, TimeUnit.SECONDS).getCause());
            assertSame(secondTimeout, secondResult.get(5, TimeUnit.SECONDS).getCause());
            verify(firstHttp).abort(firstTimeout);
            verify(secondHttp).abort(secondTimeout);
            verifyNoMoreInteractions(firstHttp, secondHttp);
        } finally {
            callers.shutdownNow();
        }
    }

    @Test
    void otherClientTypesAreReturnedUnwrapped() {
        SolrClient client = mock(SolrClient.class);
        assertSame(client, StreamingSolrClientCache.abortOnHeaderTimeout(client));
        verifyNoInteractions(client);
    }

    @Test
    void callbacksOnOtherThreadsCannotRetainOrReplaceTheCallersRequest() throws Exception {
        Http2SolrClient transport = mock(Http2SolrClient.class);
        SolrClient client = StreamingSolrClientCache.abortOnHeaderTimeout(transport);
        ArgumentCaptor<HttpListenerFactory> factory =
                ArgumentCaptor.forClass(HttpListenerFactory.class);
        verify(transport).addListenerFactory(factory.capture());
        Request ownRequest = mock(Request.class);
        Request otherRequest = mock(Request.class);
        QueryRequest query = new QueryRequest(new ModifiableSolrParams());
        SolrServerException failure = new SolrServerException(new TimeoutException("No headers"));
        ExecutorService callbackThread = Executors.newSingleThreadExecutor();
        try {
            when(transport.request(query, null))
                    .thenAnswer(
                            invocation -> {
                                factory.getValue().get().onQueued(ownRequest);
                                callbackThread
                                        .submit(
                                                () ->
                                                        factory.getValue()
                                                                .get()
                                                                .onQueued(otherRequest))
                                        .get(5, TimeUnit.SECONDS);
                                throw failure;
                            });
            assertSame(
                    failure, assertThrows(SolrServerException.class, () -> client.request(query)));
            verify(ownRequest).abort(failure.getCause());

            // Reuse the callback thread: its earlier callback must not become a stale target.
            doThrow(failure).when(transport).request(query, null);
            callbackThread
                    .submit(
                            () ->
                                    assertThrows(
                                            SolrServerException.class, () -> client.request(query)))
                    .get(5, TimeUnit.SECONDS);
            verifyNoInteractions(otherRequest);
            verifyNoMoreInteractions(ownRequest);
        } finally {
            callbackThread.shutdownNow();
        }
    }

    @Test
    void queuedCallbackOnAnotherThreadStillCapturesTheCorrectRequest() throws Exception {
        Http2SolrClient transport = mock(Http2SolrClient.class);
        SolrClient client = StreamingSolrClientCache.abortOnHeaderTimeout(transport);
        ArgumentCaptor<HttpListenerFactory> factory =
                ArgumentCaptor.forClass(HttpListenerFactory.class);
        verify(transport).addListenerFactory(factory.capture());
        Request httpRequest = mock(Request.class);
        QueryRequest query = new QueryRequest(new ModifiableSolrParams());
        SolrServerException failure = new SolrServerException(new TimeoutException("No headers"));
        ExecutorService callbackThread = Executors.newSingleThreadExecutor();
        try {
            when(transport.request(query, null))
                    .thenAnswer(
                            invocation -> {
                                HttpListenerFactory.RequestResponseListener listener =
                                        factory.getValue().get();
                                callbackThread
                                        .submit(() -> listener.onQueued(httpRequest))
                                        .get(5, TimeUnit.SECONDS);
                                throw failure;
                            });
            assertSame(
                    failure, assertThrows(SolrServerException.class, () -> client.request(query)));
            verify(httpRequest).abort(failure.getCause());
            verifyNoMoreInteractions(httpRequest);
        } finally {
            callbackThread.shutdownNow();
        }
    }

    @Test
    void callbacksOutsideTheWrapperAreIgnoredAndFirstQueuedRequestIsRetained() throws Exception {
        Http2SolrClient transport = mock(Http2SolrClient.class);
        SolrClient client = StreamingSolrClientCache.abortOnHeaderTimeout(transport);
        ArgumentCaptor<HttpListenerFactory> factory =
                ArgumentCaptor.forClass(HttpListenerFactory.class);
        verify(transport).addListenerFactory(factory.capture());
        Request bypassed = mock(Request.class);
        Request ownRequest = mock(Request.class);
        Request duplicate = mock(Request.class);
        QueryRequest query = new QueryRequest(new ModifiableSolrParams());
        SolrServerException failure = new SolrServerException(new TimeoutException("No headers"));
        factory.getValue().get().onQueued(bypassed);
        when(transport.request(query, null))
                .thenAnswer(
                        invocation -> {
                            factory.getValue().get().onQueued(ownRequest);
                            factory.getValue().get().onQueued(duplicate);
                            throw failure;
                        });
        assertSame(failure, assertThrows(SolrServerException.class, () -> client.request(query)));
        verify(ownRequest).abort(failure.getCause());
        verifyNoInteractions(bypassed, duplicate);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void rawSolrJAlreadyAbortsInterruptsAndUncheckedFailures(boolean interrupt) throws Exception {
        CountDownLatch releaseServer = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<Request> httpRequest = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        java.util.concurrent.atomic.AtomicBoolean interruptPreserved =
                new java.util.concurrent.atomic.AtomicBoolean();
        IllegalStateException parserFailure = new IllegalStateException("Parser failed");
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/solr/fail",
                exchange -> {
                    try {
                        if (!interrupt) {
                            exchange.getResponseHeaders().set("Content-Type", "application/json");
                            exchange.sendResponseHeaders(200, 0);
                            exchange.getResponseBody().write(new byte[65536]);
                            exchange.getResponseBody().flush();
                        }
                        releaseServer.await(15, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        exchange.close();
                    }
                });
        server.start();
        HttpListenerFactory observer =
                () ->
                        new HttpListenerFactory.RequestResponseListener() {
                            @Override
                            public void onQueued(Request request) {
                                httpRequest.set(request);
                            }
                        };
        try (Http2SolrClient transport =
                new Http2SolrClient.Builder(
                                "http://127.0.0.1:" + server.getAddress().getPort() + "/solr")
                        .useHttp1_1(true)
                        .withIdleTimeout(10, TimeUnit.SECONDS)
                        .withListenerFactories(List.of(observer))
                        .build()) {
            QueryRequest query = new QueryRequest(new ModifiableSolrParams());
            query.setPath("/fail");
            query.setResponseParser(
                    new NoOpResponseParser("json") {
                        @Override
                        public org.apache.solr.common.util.NamedList<Object> processResponse(
                                InputStream body, String encoding) {
                            throw parserFailure;
                        }
                    });
            Thread caller =
                    new Thread(
                            () -> {
                                try {
                                    transport.request(query);
                                } catch (Throwable e) {
                                    failure.set(e);
                                } finally {
                                    interruptPreserved.set(Thread.currentThread().isInterrupted());
                                    finished.countDown();
                                }
                            });
            try {
                caller.start();
                if (interrupt) {
                    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                    boolean waitingForHeaders;
                    do {
                        waitingForHeaders =
                                java.util.Arrays.stream(caller.getStackTrace())
                                        .anyMatch(
                                                frame ->
                                                        frame.getClassName()
                                                                        .endsWith(
                                                                                "InputStreamResponseListener")
                                                                && frame.getMethodName()
                                                                        .equals("get"));
                        if (!waitingForHeaders) {
                            java.util.concurrent.locks.LockSupport.parkNanos(
                                    TimeUnit.MILLISECONDS.toNanos(10));
                        }
                    } while (!waitingForHeaders
                            && caller.isAlive()
                            && System.nanoTime() < deadline);
                    assertTrue(
                            waitingForHeaders,
                            "Caller must be waiting for headers before interruption");
                    caller.interrupt();
                }
                assertTrue(finished.await(5, TimeUnit.SECONDS));
                assertNotNull(httpRequest.get());
                if (interrupt) {
                    assertInstanceOf(RuntimeException.class, failure.get());
                    assertInstanceOf(InterruptedException.class, failure.get().getCause());
                    assertTrue(interruptPreserved.get());
                    assertSame(failure.get().getCause(), httpRequest.get().getAbortCause());
                } else {
                    assertInstanceOf(RuntimeException.class, failure.get());
                    assertSame(parserFailure, failure.get().getCause());
                    assertNotNull(httpRequest.get().getAbortCause());
                }
            } finally {
                caller.interrupt();
                caller.join(5000);
            }
        } finally {
            releaseServer.countDown();
            server.stop(0);
        }
    }
}
