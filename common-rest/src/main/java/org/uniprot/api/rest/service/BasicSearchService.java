package org.uniprot.api.rest.service;

import org.springframework.context.annotation.PropertySource;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.result.Cursor;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.store.search.DefaultSearchHandler;
import org.uniprot.store.search.document.Document;

import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @param <T>
 * @param <R>
 * @author lgonzales
 */


@PropertySource( "classpath:common-message.properties")
public class BasicSearchService<T, R extends Document> {
    private final SolrQueryRepository<R> repository;
    private final Function<R, T> entryConverter;

    public BasicSearchService(SolrQueryRepository<R> repository, Function<R, T> entryConverter) {
        this.repository = repository;
        this.entryConverter = entryConverter;
    }

    public T getEntity(String idField, String value) {
        try {
            String query = idField + ":" + value;
            SolrRequest solrRequest = SolrRequest.builder().query(query).build();
            R document = repository.getEntry(solrRequest)
                    .orElseThrow(() -> new ResourceNotFoundException("{search.not.found}"));

            T entry = entryConverter.apply(document);
            if (entry == null) {
                String message = entryConverter.getClass() + " can not convert object for: [" + value + "]";
                throw new ServiceException(message);
            } else {
                return entry;
            }
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            String message = "Could not get entity for id: [" + value + "]";
            throw new ServiceException(message, e);
        }
    }

    public QueryResult<T> search(SolrRequest request, String cursor, int pageSize) {
        QueryResult<R> results = repository.searchPage(request, cursor, pageSize);
        List<T> converted = results.getContent().stream()
                .map(entryConverter)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return QueryResult.of(converted, results.getPage(), results.getFacets());
    }

    public Stream<T> download(SolrRequest request) {
        Cursor<R> results = repository.getAll(request);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(results, Spliterator.ORDERED), false)
                .map(entryConverter)
                .filter(Objects::nonNull);
    }

    public SolrRequest createSolrRequest(SearchRequest request, FacetConfig facetConfig,
                                         AbstractSolrSortClause solrSortClause, DefaultSearchHandler defaultSearchHandler) {
        SolrRequest.SolrRequestBuilder builder = createSolrRequestBuilder(request, facetConfig, solrSortClause, defaultSearchHandler);
        return builder.build();
    }

    private SolrRequest.SolrRequestBuilder createSolrRequestBuilder(SearchRequest request, FacetConfig facetConfig,
                                                                    AbstractSolrSortClause solrSortClause, DefaultSearchHandler defaultSearchHandler) {
        SolrRequest.SolrRequestBuilder requestBuilder = SolrRequest.builder();
        String requestedQuery = request.getQuery();

        boolean hasScore = false;
        if (defaultSearchHandler != null && defaultSearchHandler.hasDefaultSearch(requestedQuery)) {
            requestedQuery = defaultSearchHandler.optimiseDefaultSearch(requestedQuery);
            hasScore = true;
            requestBuilder.defaultQueryOperator(Query.Operator.OR);
        }
        requestBuilder.query(requestedQuery);

        requestBuilder.addSort(solrSortClause.getSort(request.getSort(), hasScore));

        if (request.hasFacets()) {
            requestBuilder.facets(request.getFacetList());
            requestBuilder.facetConfig(facetConfig);
        }

        return requestBuilder;
    }
}
