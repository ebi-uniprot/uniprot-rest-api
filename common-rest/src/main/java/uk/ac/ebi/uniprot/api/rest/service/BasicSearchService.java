package uk.ac.ebi.uniprot.api.rest.service;

import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.result.Cursor;
import uk.ac.ebi.uniprot.api.common.exception.ResourceNotFoundException;
import uk.ac.ebi.uniprot.api.common.exception.ServiceException;
import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrQueryBuilder;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrQueryRepository;
import uk.ac.ebi.uniprot.api.common.repository.search.facet.GenericFacetConfig;
import uk.ac.ebi.uniprot.api.rest.request.SearchRequest;
import uk.ac.ebi.uniprot.api.rest.search.AbstractSolrSortClause;
import uk.ac.ebi.uniprot.search.DefaultSearchHandler;
import uk.ac.ebi.uniprot.search.document.Document;

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
 */
public class BasicSearchService<T, R extends Document> {

    private final SolrQueryRepository<R> repository;
    private final Function<R, T> entryConverter;

    public BasicSearchService(SolrQueryRepository<R> repository, Function<R, T> entryConverter) {
        this.repository = repository;
        this.entryConverter = entryConverter;
    }

    public T getEntity(String idField, String value) {
        try {
            SimpleQuery query = new SimpleQuery(Criteria.where(idField).is(value));
            R document = repository.getEntry(query)
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

    public QueryResult<T> search(SimpleQuery query, String cursor, int pageSize) {
        QueryResult<R> results = repository.searchPage(query, cursor, pageSize);
        List<T> converted = results.getContent().stream()
                .map(entryConverter)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return QueryResult.of(converted, results.getPage(), results.getFacets());
    }

    public Stream<T> download(SimpleQuery query) {
        Cursor<R> results = repository.getAll(query);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(results, Spliterator.ORDERED), false)
                .map(entryConverter)
                .filter(Objects::nonNull);
    }

    public SimpleQuery createSolrQuery(SearchRequest request, GenericFacetConfig facetConfig,
                                       AbstractSolrSortClause solrSortClause, DefaultSearchHandler defaultSearchHandler) {
        SolrQueryBuilder builder = createSolrQueryBuilder(request, facetConfig, solrSortClause, defaultSearchHandler);
        return builder.build();
    }

    public SolrQueryBuilder createSolrQueryBuilder(SearchRequest request, GenericFacetConfig facetConfig,
                                                   AbstractSolrSortClause solrSortClause, DefaultSearchHandler defaultSearchHandler) {
        SolrQueryBuilder builder = new SolrQueryBuilder();
        String requestedQuery = request.getQuery();

        boolean hasScore = false;
        if (defaultSearchHandler != null && defaultSearchHandler.hasDefaultSearch(requestedQuery)) {
            requestedQuery = defaultSearchHandler.optimiseDefaultSearch(requestedQuery);
            hasScore = true;
            builder.defaultOperator(Query.Operator.OR);
        }
        builder.query(requestedQuery);

        builder.addSort(solrSortClause.getSort(request.getSort(), hasScore));

        if (request.hasFacets()) {
            builder.facets(request.getFacetList());
            builder.facetConfig(facetConfig);
        }

        return builder;
    }

}
