package org.uniprot.api.uniprotkb.common.repository.search;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.apache.solr.client.solrj.request.json.JsonQueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.CursorMarkParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.rest.respository.facet.impl.UniProtKBFacetConfig;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.Document;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

/**
 * Repository responsible to query SolrCollection.uniprot
 *
 * @author lgonzales
 */
@Repository
public class UniprotQueryRepository extends SolrQueryRepository<UniProtDocument> {
    private static final Logger SHADOW_LOGGER = LoggerFactory.getLogger("solr9-shadow");
    private static final int IDS_TO_LOG = 25;

    // Percentage of requests where a solr=9 shadow attempt is made (0-100).
    // The solr=8 log line is ONLY emitted when this sampling decision is "in" -
    // so every solr=8 line in the log is guaranteed to have a solr=9 counterpart
    // (success, failed, or skipped=true reason=queue_full).
    private static final int SHADOW_SAMPLE_PERCENT = 10;

    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();
    private static final AtomicLong SHADOW_ID_COUNTER = new AtomicLong();
    private static final ThreadLocal<Long> CURRENT_SHADOW_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_SOLR_PARAMS = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> CURRENT_SHADOW_SAMPLED = new ThreadLocal<>();

    private static final ExecutorService SHADOW_EXECUTOR =
            new ThreadPoolExecutor(
                    1,
                    2,
                    30L,
                    TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(100),
                    runnable -> {
                        Thread thread =
                                new Thread(
                                        runnable,
                                        "solr9-shadow-" + THREAD_COUNTER.incrementAndGet());
                        thread.setDaemon(true);
                        return thread;
                    },
                    new ThreadPoolExecutor.AbortPolicy());

    static {
        SHADOW_LOGGER.info(
                "solr9-shadow sampling initialized: SHADOW_SAMPLE_PERCENT={}",
                SHADOW_SAMPLE_PERCENT);
    }

    private final SolrClient solr9Client;
    private final SolrRequestConverter requestConverter;
    private final TupleStreamTemplate solr9TupleStreamTemplate;

    public UniprotQueryRepository(
            @Qualifier("uniProtKBSolrClient") SolrClient solrClient,
            @Qualifier("uniProtKBSolr9Client") ObjectProvider<SolrClient> solr9Client,
            @Qualifier("uniProtKBSolr9TupleStream")
                    ObjectProvider<TupleStreamTemplate> solr9TupleStreamTemplate,
            UniProtKBFacetConfig facetConfig,
            SolrRequestConverter requestConverter) {
        super(
                solrClient,
                SolrCollection.uniprot,
                UniProtDocument.class,
                facetConfig,
                requestConverter);
        this.solr9Client = solr9Client.getIfAvailable();
        this.solr9TupleStreamTemplate = solr9TupleStreamTemplate.getIfAvailable();
        this.requestConverter = requestConverter;
    }

    @Override
    public QueryResult<UniProtDocument> searchPage(SolrRequest request, String cursor) {
        long shadowId = SHADOW_ID_COUNTER.incrementAndGet();
        boolean sampled =
                solr9Client != null
                        && ThreadLocalRandom.current().nextInt(100) < SHADOW_SAMPLE_PERCENT;
        try {
            CURRENT_SHADOW_ID.set(shadowId);
            CURRENT_SHADOW_SAMPLED.set(sampled);
            QueryResult<UniProtDocument> result = super.searchPage(request, cursor);
            if (sampled) {
                shadowSearchPage(
                        shadowId,
                        request,
                        cursor,
                        CursorPage.of(cursor, request.getRows()).getCursor());
            }
            return result;
        } finally {
            CURRENT_SHADOW_ID.remove();
            CURRENT_SHADOW_SAMPLED.remove();
            CURRENT_SOLR_PARAMS.remove();
        }
    }

    @Override
    protected void logConvertedSolrRequest(
            SolrRequest request, String cursor, JsonQueryRequest solrQuery) {
        CURRENT_SOLR_PARAMS.set(String.valueOf(solrQuery.getParams()));
    }

    @SuppressWarnings(
            "javasecurity:S5145") // ignore it for now. logging is temporary and we validate the
    // query
    // first and other params anyway
    @Override
    protected void logSearchResponse(
            SolrRequest request,
            String cursor,
            QueryResponse solrResponse,
            List<UniProtDocument> documents) {
        Long shadowId = CURRENT_SHADOW_ID.get();
        Boolean sampled = CURRENT_SHADOW_SAMPLED.get();
        if (shadowId != null && Boolean.TRUE.equals(sampled)) {
            String sanitizedQuery = getSanitizedQuery(request);
            SHADOW_LOGGER.info(
                    "shadowId={} solr=8 collection=uniprot numFound={} qTime={} cursor={} ids={} params={} query={} filters={} sorts={} rows={} facets={}",
                    shadowId,
                    solrResponse.getResults().getNumFound(),
                    solrResponse.getQTime(),
                    cursor,
                    getDocumentIds(documents),
                    CURRENT_SOLR_PARAMS.get(),
                    sanitizedQuery,
                    request.getFilterQueries(),
                    request.getSorts(),
                    request.getRows(),
                    request.getFacets());
        }
    }

    public void shadowSearchPage(
            long shadowId, SolrRequest request, String cursor, String solrCursor) {

        if (solr9Client == null) {
            return;
        }
        String sanitizedQuery = getSanitizedQuery(request);
        String currentParams = CURRENT_SOLR_PARAMS.get();
        try {
            SHADOW_EXECUTOR.execute(
                    () -> {
                        try {
                            JsonQueryRequest solrQuery = getJsonQueryRequest(request, solrCursor);
                            QueryResponse response =
                                    solrQuery.process(
                                            solr9Client, SolrCollection.uniprot.toString());
                            SHADOW_LOGGER.info(
                                    "shadowId={} solr=9 collection=uniprot numFound={} qTime={} cursor={} ids={} params={} query={} filters={} sorts={} rows={} facets={}",
                                    shadowId,
                                    response.getResults().getNumFound(),
                                    response.getQTime(),
                                    cursor,
                                    getDocumentIds(response.getBeans(UniProtDocument.class)),
                                    solrQuery.getParams(),
                                    sanitizedQuery,
                                    request.getFilterQueries(),
                                    request.getSorts(),
                                    request.getRows(),
                                    request.getFacets());
                        } catch (Exception e) {
                            SHADOW_LOGGER.warn(
                                    "shadowId={} solr=9 collection=uniprot failed cursor={} params={} query={} filters={} sorts={} rows={} facets={}",
                                    shadowId,
                                    cursor,
                                    currentParams,
                                    sanitizedQuery,
                                    request.getFilterQueries(),
                                    request.getSorts(),
                                    request.getRows(),
                                    request.getFacets(),
                                    e);
                        }
                    });
        } catch (RejectedExecutionException e) {
            SHADOW_LOGGER.warn(
                    "shadowId={} solr=9 collection=uniprot skipped=true reason=queue_full cursor={} params={} query={} filters={} sorts={} rows={} facets={}",
                    shadowId,
                    cursor,
                    currentParams,
                    sanitizedQuery,
                    request.getFilterQueries(),
                    request.getSorts(),
                    request.getRows(),
                    request.getFacets());
        }
    }

    private @NonNull JsonQueryRequest getJsonQueryRequest(SolrRequest request, String solrCursor) {
        JsonQueryRequest solrQuery = requestConverter.toJsonQueryRequest(request);
        ModifiableSolrParams params = (ModifiableSolrParams) solrQuery.getParams();
        if (solrCursor != null && !solrCursor.isEmpty()) {
            params.set(CursorMarkParams.CURSOR_MARK_PARAM, solrCursor);
        } else {
            params.set(CursorMarkParams.CURSOR_MARK_PARAM, CursorMarkParams.CURSOR_MARK_START)
                    .set(SPELLCHECK_PARAM, true);
        }
        return solrQuery;
    }

    public void shadowStream(long shadowId, SolrRequest request) {
        if (solr9TupleStreamTemplate == null) {
            return;
        }
        String sanitizedQuery = getSanitizedQuery(request);

        try {
            SHADOW_EXECUTOR.execute(
                    () -> {
                        TupleStream tupleStream = solr9TupleStreamTemplate.create(request);
                        long count = 0;
                        List<String> firstIds = new ArrayList<>();
                        LinkedList<String> lastIds = new LinkedList<>();
                        try {
                            tupleStream.open();
                            Tuple tuple = tupleStream.read();
                            while (!tuple.EOF) {
                                count++;
                                String id = tuple.getString("accession_id");
                                if (firstIds.size() < 10) {
                                    firstIds.add(id);
                                }
                                lastIds.add(id);
                                if (lastIds.size() > 10) {
                                    lastIds.removeFirst();
                                }
                                tuple = tupleStream.read();
                            }
                            SHADOW_LOGGER.info(
                                    "shadowId={} solr=9 collection=uniprot streamCount={} firstIds={} lastIds={} query={} filters={} sorts={}",
                                    shadowId,
                                    count,
                                    firstIds,
                                    lastIds,
                                    sanitizedQuery,
                                    request.getFilterQueries(),
                                    request.getSorts());
                        } catch (Exception e) {
                            SHADOW_LOGGER.warn(
                                    "shadowId={} solr=9 collection=uniprot streamFailed query={} filters={} sorts={}",
                                    shadowId,
                                    sanitizedQuery,
                                    request.getFilterQueries(),
                                    request.getSorts(),
                                    e);
                        } finally {
                            try {
                                tupleStream.close();
                            } catch (Exception e) {
                                // ignore
                            }
                        }
                    });
        } catch (RejectedExecutionException e) {
            SHADOW_LOGGER.warn(
                    "shadowId={} solr=9 collection=uniprot streamSkipped=true reason=queue_full query={} filters={} sorts={}",
                    shadowId,
                    sanitizedQuery,
                    request.getFilterQueries(),
                    request.getSorts());
        }
    }

    public void logSolr8Stream(
            long shadowId,
            SolrRequest request,
            long count,
            List<String> firstIds,
            List<String> lastIds) {
        String sanitizedQuery = getSanitizedQuery(request);
        SHADOW_LOGGER.info(
                "shadowId={} solr=8 collection=uniprot streamCount={} firstIds={} lastIds={} query={} filters={} sorts={}",
                shadowId,
                count,
                firstIds,
                lastIds,
                sanitizedQuery,
                request.getFilterQueries(),
                request.getSorts());
    }

    public long getShadowId() {
        return SHADOW_ID_COUNTER.incrementAndGet();
    }

    public boolean isSampled() {
        return solr9TupleStreamTemplate != null
                && ThreadLocalRandom.current().nextInt(100) < SHADOW_SAMPLE_PERCENT;
    }

    private List<String> getDocumentIds(List<? extends Document> documents) {
        return documents.stream().limit(IDS_TO_LOG).map(Document::getDocumentId).toList();
    }

    private static @NonNull String getSanitizedQuery(SolrRequest request) {
        String sanitizedQuery =
                request.getQuery() == null
                        ? "null"
                        : request.getQuery().replaceAll("[\r\n\t]", "_");
        return sanitizedQuery;
    }
}
