package org.uniprot.api.rest.service;

import static org.uniprot.api.rest.output.PredefinedAPIStatus.LEADING_WILDCARD_IGNORED;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.context.annotation.PropertySource;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.common.repository.search.*;
import org.uniprot.api.common.repository.stream.document.DefaultDocumentIdStream;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.SolrQueryUtil;
import org.uniprot.store.search.document.Document;

/**
 * @param <D> the type of the input to the class. a type of Document
 * @param <R> the type of the result of the class
 * @author lgonzales
 */
@PropertySource("classpath:common-message.properties")
public abstract class BasicSearchService<D extends Document, R> {
    private final SolrQueryRepository<D> repository;
    private final Function<D, R> entryConverter;
    private final RequestConverter requestConverter;

    public BasicSearchService(
            SolrQueryRepository<D> repository,
            Function<D, R> entryConverter,
            RequestConverter requestConverter) {
        this.repository = repository;
        this.entryConverter = entryConverter;
        this.requestConverter = requestConverter;
    }

    public R findByUniqueId(final String uniqueId) {
        return getEntity(getIdField().getFieldName(), uniqueId);
    }

    public R getEntity(String idField, String value) {
        try {
            String query = idField + ":" + value;
            SolrRequest solrRequest =
                    SolrRequest.builder().query(query).rows(NumberUtils.INTEGER_ONE).build();
            D document =
                    repository
                            .getEntry(solrRequest)
                            .orElseThrow(() -> new ResourceNotFoundException("{search.not.found}"));

            R entry = entryConverter.apply(document);
            if (entry == null) {
                String message =
                        entryConverter.getClass() + " can not convert object for: [" + value + "]";
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

    public QueryResult<R> search(SearchRequest request) {
        SolrRequest solrRequest = requestConverter.createSearchSolrRequest(request);
        QueryResult<D> results = repository.searchPage(solrRequest, request.getCursor());

        Stream<R> converted = convertDocumentsToEntries(request, results);

        Set<ProblemPair> warnings = getWarnings(request.getQuery(), Set.of());
        return QueryResult.<R>builder()
                .content(converted)
                .page(results.getPage())
                .facets(results.getFacets())
                .suggestions(results.getSuggestions())
                .warnings(warnings)
                .build();
    }

    protected Stream<R> convertDocumentsToEntries(SearchRequest request, QueryResult<D> results) {
        return results.getContent().map(entryConverter).filter(Objects::nonNull);
    }

    public Stream<R> stream(StreamRequest request) {
        SolrRequest solrRequest = requestConverter.createStreamSolrRequest(request);

        return repository
                .getAll(solrRequest)
                .map(entryConverter)
                .filter(Objects::nonNull)
                .limit(solrRequest.getTotalRows());
    }

    protected abstract SearchFieldItem getIdField();

    public Stream<String> streamRdf(StreamRequest streamRequest, String dataType, String format) {
        SolrRequest solrRequest = requestConverter.createStreamSolrRequest(streamRequest);
        List<String> idStream = getDocumentIdStream().fetchIds(solrRequest).toList();
        return getRdfStreamer().stream(idStream.stream(), dataType, format);
    }

    protected DefaultDocumentIdStream<D> getDocumentIdStream() {
        throw new UnsupportedOperationException("Override this method");
    }

    public String getRdf(String id, String dataType, String format) {
        return getRdfStreamer().stream(Stream.of(id), dataType, format)
                .collect(Collectors.joining());
    }

    protected RdfStreamer getRdfStreamer() {
        throw new UnsupportedOperationException("Override this method");
    }

    protected Set<ProblemPair> getWarnings(String query, Set<String> leadWildcardSupportedFields) {
        ProblemPair warning = getLeadingWildcardIgnoredWarning(query, leadWildcardSupportedFields);
        Set<ProblemPair> warnings = Objects.isNull(warning) ? null : Set.of(warning);
        return warnings;
    }

    private ProblemPair getLeadingWildcardIgnoredWarning(
            String query, Set<String> leadWildcardSupportedFields) {
        if (SolrQueryUtil.ignoreLeadingWildcard(query, leadWildcardSupportedFields)) {
            return new ProblemPair(
                    LEADING_WILDCARD_IGNORED.getCode(), LEADING_WILDCARD_IGNORED.getMessage());
        }
        return null;
    }

    protected RequestConverter getRequestConverter() {
        return requestConverter;
    }
}
