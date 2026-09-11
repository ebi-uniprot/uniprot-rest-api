package org.uniprot.api.uniprotkb.common.repository.search;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.request.json.JsonQueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.CursorMarkParams;
import org.apache.solr.common.params.ModifiableSolrParams;
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
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();
    private static final AtomicLong SHADOW_ID_COUNTER = new AtomicLong();
    private static final ThreadLocal<Long> CURRENT_SHADOW_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_SOLR_PARAMS = new ThreadLocal<>();
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

    private final SolrClient solr9Client;
    private final SolrRequestConverter requestConverter;

    public UniprotQueryRepository(
            @Qualifier("uniProtKBSolrClient") SolrClient solrClient,
            @Qualifier("uniProtKBSolr9Client") ObjectProvider<SolrClient> solr9Client,
            UniProtKBFacetConfig facetConfig,
            SolrRequestConverter requestConverter) {
        super(
                solrClient,
                SolrCollection.uniprot,
                UniProtDocument.class,
                facetConfig,
                requestConverter);
        this.solr9Client = solr9Client.getIfAvailable();
        this.requestConverter = requestConverter;
    }

    @Override
    public QueryResult<UniProtDocument> searchPage(SolrRequest request, String cursor) {
        long shadowId = SHADOW_ID_COUNTER.incrementAndGet();
        try {
            CURRENT_SHADOW_ID.set(shadowId);
            QueryResult<UniProtDocument> result = super.searchPage(request, cursor);
            shadowSearchPage(
                    shadowId,
                    request,
                    cursor,
                    CursorPage.of(cursor, request.getRows()).getCursor());
            return result;
        } finally {
            CURRENT_SHADOW_ID.remove();
            CURRENT_SOLR_PARAMS.remove();
        }
    }

    @Override
    protected void logConvertedSolrRequest(
            SolrRequest request, String cursor, JsonQueryRequest solrQuery) {
        CURRENT_SOLR_PARAMS.set(String.valueOf(solrQuery.getParams()));
    }

    @Override
    protected void logSearchResponse(
            SolrRequest request,
            String cursor,
            QueryResponse solrResponse,
            List<UniProtDocument> documents) {
        Long shadowId = CURRENT_SHADOW_ID.get();
        if (shadowId != null) {
            SHADOW_LOGGER.info(
                    "shadowId={} solr=8 collection=uniprot numFound={} qTime={} cursor={} ids={} params={} query={} filters={} sorts={} rows={} facets={}",
                    shadowId,
                    solrResponse.getResults().getNumFound(),
                    solrResponse.getQTime(),
                    cursor,
                    getDocumentIds(documents),
                    CURRENT_SOLR_PARAMS.get(),
                    request.getQuery(),
                    request.getFilterQueries(),
                    request.getSorts(),
                    request.getRows(),
                    request.getFacets());
        }
    }

    private void shadowSearchPage(
            long shadowId, SolrRequest request, String cursor, String solrCursor) {
        if (solr9Client == null) {
            return;
        }
        try {
            SHADOW_EXECUTOR.execute(
                    () -> {
                        try {
                            JsonQueryRequest solrQuery =
                                    requestConverter.toJsonQueryRequest(request);
                            ModifiableSolrParams params =
                                    (ModifiableSolrParams) solrQuery.getParams();
                            if (solrCursor != null && !solrCursor.isEmpty()) {
                                params.set(CursorMarkParams.CURSOR_MARK_PARAM, solrCursor);
                            } else {
                                params.set(
                                                CursorMarkParams.CURSOR_MARK_PARAM,
                                                CursorMarkParams.CURSOR_MARK_START)
                                        .set(SPELLCHECK_PARAM, true);
                            }
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
                                    request.getQuery(),
                                    request.getFilterQueries(),
                                    request.getSorts(),
                                    request.getRows(),
                                    request.getFacets());
                        } catch (Exception e) {
                            SHADOW_LOGGER.warn(
                                    "shadowId={} solr=9 collection=uniprot failed cursor={} query={} filters={} sorts={} rows={} facets={}",
                                    shadowId,
                                    cursor,
                                    request.getQuery(),
                                    request.getFilterQueries(),
                                    request.getSorts(),
                                    request.getRows(),
                                    request.getFacets(),
                                    e);
                        }
                    });
        } catch (RejectedExecutionException e) {
            SHADOW_LOGGER.warn(
                    "shadowId={} solr=9 collection=uniprot skipped=true reason=queue_full cursor={} query={} filters={} sorts={} rows={} facets={}",
                    shadowId,
                    cursor,
                    request.getQuery(),
                    request.getFilterQueries(),
                    request.getSorts(),
                    request.getRows(),
                    request.getFacets());
        }
    }

    private List<String> getDocumentIds(List<? extends Document> documents) {
        return documents.stream().limit(IDS_TO_LOG).map(Document::getDocumentId).toList();
    }
}
