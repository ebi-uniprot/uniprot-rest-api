package org.uniprot.api.common.repository.search;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.json.JsonQueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.CursorMarkParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uniprot.api.common.exception.InvalidRequestException;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.search.facet.FacetResponseConverter;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.common.repository.search.suggestion.Suggestion;
import org.uniprot.api.common.repository.search.suggestion.SuggestionConverter;
import org.uniprot.api.common.repository.search.term.TermInfo;
import org.uniprot.api.common.repository.search.term.TermInfoConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.Document;

/**
 * Solr Basic Repository class to enable the execution of dynamically build queries in a solr
 * collections.
 *
 * <p>It was defined a common QueryResult object in order to be able to
 *
 * @param <T> Returned Solr entity
 * @author lgonzales
 */
public abstract class SolrQueryRepository<T extends Document> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SolrQueryRepository.class);
    public static final String SPELLCHECK_PARAM = "spellcheck";
    private final TermInfoConverter termInfoConverter;
    private final SolrRequestConverter requestConverter;

    private final SolrClient solrClient;
    private final SolrCollection collection;
    private final Class<T> tClass;
    private final FacetResponseConverter facetConverter;
    private final SuggestionConverter suggestionConverter;

    protected SolrQueryRepository(
            SolrClient solrClient,
            SolrCollection collection,
            Class<T> tClass,
            FacetConfig facetConfig,
            SolrRequestConverter requestConverter) {
        this.solrClient = solrClient;
        this.collection = collection;
        this.tClass = tClass;
        this.facetConverter = new FacetResponseConverter(facetConfig);
        this.requestConverter = requestConverter;
        this.termInfoConverter = new TermInfoConverter();
        this.suggestionConverter = new SuggestionConverter();
    }

    public QueryResult<T> searchPage(SolrRequest request, String cursor) {
        try {
            CursorPage page = CursorPage.of(cursor, request.getRows());
            QueryResponse solrResponse = search(request, page.getCursor());

            List<T> resultList = getResponseDocuments(solrResponse);
            page.setNextCursor(solrResponse.getNextCursorMark());
            page.setTotalElements(solrResponse.getResults().getNumFound());

            List<Facet> facets = facetConverter.convert(solrResponse, request.getFacets());
            List<TermInfo> termInfos = termInfoConverter.convert(solrResponse);
            List<Suggestion> suggestions = suggestionConverter.convert(solrResponse);

            return QueryResult.of(resultList.stream(), page, facets, termInfos, null, suggestions);
        } catch (InvalidRequestException ire) {
            throw ire;
        } catch (Exception e) {
            if (e.getCause() instanceof InvalidRequestException) {
                throw (InvalidRequestException) e.getCause();
            }
            throw new QueryRetrievalException(
                    "Unexpected error retrieving data from our Repository", e);
        } finally {
            logSolrQuery(request);
        }
    }

    public Optional<T> getEntry(SolrRequest request) {
        try {
            JsonQueryRequest solrQuery = requestConverter.toJsonQueryRequest(request, true);
            QueryResponse response = solrQuery.process(solrClient, collection.toString());
            if (!response.getResults().isEmpty()) {
                if (response.getResults().size() > 1) {
                    LOGGER.warn(
                            "More than 1 result found for a single result query, returning first entry in list");
                }
                return Optional.ofNullable(getResponseDocuments(response).get(0));
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            throw new QueryRetrievalException("Error executing solr query", e);
        } finally {
            logSolrQuery(request);
        }
    }

    public Stream<T> getAll(SolrRequest request) {
        SolrResultsIterator<T> resultsIterator =
                new SolrResultsIterator<>(
                        solrClient,
                        collection,
                        requestConverter.toJsonQueryRequest(request),
                        tClass);
        return StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(resultsIterator, Spliterator.ORDERED),
                        false)
                .flatMap(Collection::stream);
    }

    public QueryResponse query(SolrQuery solrQuery) {
        try {
            return solrClient.query(collection.name(), solrQuery);
        } catch (SolrServerException | IOException e) {
            throw new QueryRetrievalException(
                    "Unexpected error retrieving data from our Repository", e);
        }
    }

    protected List<T> getResponseDocuments(QueryResponse solrResponse) {
        return solrResponse.getBeans(tClass);
    }

    private QueryResponse search(SolrRequest request, String cursor)
            throws IOException, SolrServerException {
        JsonQueryRequest solrQuery = requestConverter.toJsonQueryRequest(request);
        if (cursor != null && !cursor.isEmpty()) {
            ((ModifiableSolrParams) solrQuery.getParams())
                    .set(CursorMarkParams.CURSOR_MARK_PARAM, cursor);
        } else {
            ((ModifiableSolrParams) solrQuery.getParams())
                    .set(CursorMarkParams.CURSOR_MARK_PARAM, CursorMarkParams.CURSOR_MARK_START)
                    .set(SPELLCHECK_PARAM, true);
        }
        return solrQuery.process(solrClient, collection.toString());
    }

    private void logSolrQuery(SolrRequest request) {
        if (request != null) {
            LOGGER.debug("SolrRequest: {}", request);
        }
    }
}
