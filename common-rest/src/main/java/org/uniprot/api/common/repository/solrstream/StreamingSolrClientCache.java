package org.uniprot.api.common.repository.solrstream;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.impl.HttpListenerFactory;
import org.apache.solr.client.solrj.io.SolrClientCache;
import org.apache.solr.common.util.NamedList;
import org.eclipse.jetty.client.api.Request;

import lombok.extern.slf4j.Slf4j;

/**
 * Workaround for SolrJ 9.10.1's synchronous response-header timeout not aborting its HTTP request.
 * StreamingSolrClientCacheTest checks the raw client's behavior so an upstream fix prompts removal.
 *
 * <p>Related cancellation constraints: <a href="https://issues.apache.org/jira/browse/SOLR-17819">
 * SOLR-17819</a> concerns asynchronous HTTP/2 cancellation, not this synchronous timeout gap.
 */
@Slf4j
class StreamingSolrClientCache extends SolrClientCache {
    private final Map<String, SolrClient> streamingClients = new HashMap<>();

    @Override
    public synchronized SolrClient getHttpSolrClient(String baseUrl) {
        SolrClient client = super.getHttpSolrClient(baseUrl);
        return streamingClients.computeIfAbsent(baseUrl, ignored -> abortOnHeaderTimeout(client));
    }

    @Override
    public synchronized void close() {
        try {
            // The superclass owns and closes the underlying clients.
            super.close();
        } finally {
            streamingClients.clear();
        }
    }

    @SuppressWarnings("deprecation") // SolrClientCache creates the underlying clients internally.
    static SolrClient abortOnHeaderTimeout(SolrClient client) {
        if (!(client instanceof Http2SolrClient httpClient)) {
            log.warn(
                    "Streaming timeout workaround is not applied to client type {}",
                    client.getClass().getName());
            return client;
        }
        ThreadLocal<AtomicReference<Request>> currentCall = new ThreadLocal<>();
        httpClient.addListenerFactory(
                () -> {
                    // SolrJ constructs listeners inside request(). Capture the active call here,
                    // rather than reading a ThreadLocal from a potentially different callback
                    // thread.
                    AtomicReference<Request> holder = currentCall.get();
                    if (holder == null) {
                        currentCall.remove();
                    }
                    return new HttpListenerFactory.RequestResponseListener() {
                        @Override
                        public void onQueued(Request request) {
                            if (holder != null) {
                                holder.compareAndSet(null, request);
                            }
                        }
                    };
                });
        return new SolrClient() {
            @Override
            public NamedList<Object> request(SolrRequest<?> request, String collection)
                    throws SolrServerException, IOException {
                AtomicReference<Request> holder = new AtomicReference<>();
                currentCall.set(holder);
                try {
                    return client.request(request, collection);
                } catch (SolrServerException e) {
                    // SolrJ 9.10.1 does not abort the HTTP request when listener.get() times out.
                    // Queued requests can otherwise run later with nobody to close their response.
                    if (e.getCause() instanceof TimeoutException) {
                        Request httpRequest = holder.get();
                        if (httpRequest != null) {
                            httpRequest.abort(e.getCause());
                        }
                    }
                    throw e;
                } finally {
                    holder.set(null);
                    currentCall.remove();
                }
            }

            @Override
            public void close() throws IOException {
                client.close();
            }
        };
    }
}
