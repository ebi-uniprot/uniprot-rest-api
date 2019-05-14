package uk.ac.ebi.uniprot.api.common.service;

import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.http.MediaType;
import uk.ac.ebi.uniprot.api.common.exception.ResourceNotFoundException;
import uk.ac.ebi.uniprot.api.common.exception.ServiceException;
import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrQueryBuilder;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrQueryRepository;
import uk.ac.ebi.uniprot.api.common.repository.search.facet.GenericFacetConfig;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.api.rest.request.SearchRequest;
import uk.ac.ebi.uniprot.api.rest.search.AbstractSolrSortClause;
import uk.ac.ebi.uniprot.search.DefaultSearchHandler;
import uk.ac.ebi.uniprot.search.document.Document;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE;

/**
 *
 *
 * @param <T>
 * @param <R>
 */
public class SearchRetrievalService<T,R extends Document> {

    private final SolrQueryRepository<R> repository;
    private final Function<R,T> entryConverter;

    public SearchRetrievalService(SolrQueryRepository<R> repository, Function<R,T> entryConverter){
        this.repository = repository;
        this.entryConverter = entryConverter;
    }

    public T getEntity(String idField, String value){
        try {
            SimpleQuery query = new SimpleQuery(Criteria.where(idField).is(value));
            Optional<R> optionalDoc = repository.getEntry(query);
            if(optionalDoc.isPresent()) {
                T entry = entryConverter.apply(optionalDoc.get());
                if(entry ==null) {
                    String message = entryConverter.getClass()+ " can not convert object for: [" + value + "]";
                    throw new ServiceException(message);
                } else {
                    return entry;
                }
            }else {
                throw new ResourceNotFoundException("{search.not.found}");
            }
        }catch (ResourceNotFoundException e) {
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

    public void searchForDownload(SimpleQuery query, String cursor, int pageSize, MessageConverterContext<T> context) {
        MediaType contentType = context.getContentType();

        QueryResult<R> results = repository.searchPage(query, cursor, pageSize);

        if (contentType.equals(LIST_MEDIA_TYPE)) {
            List<String> idList = results.getContent().stream()
                    .map(Document::getDocumentId)
                    .collect(Collectors.toList());
            context.setEntityIds(idList.stream());
        } else {
            List<T> converted = results.getContent()
                    .stream().map(entryConverter)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            context.setEntities(converted.stream());
        }
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
        if(defaultSearchHandler != null && defaultSearchHandler.hasDefaultSearch(requestedQuery)){
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
