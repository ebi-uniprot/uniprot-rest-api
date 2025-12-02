package org.uniprot.api.support.data.common.taxonomy.service;

import java.util.Set;
import java.util.stream.Stream;

import javax.validation.Valid;

import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.stream.document.DefaultDocumentIdStream;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.api.support.data.common.taxonomy.repository.TaxonomyRepository;
import org.uniprot.api.support.data.common.taxonomy.request.GetByTaxonIdsRequest;
import org.uniprot.api.support.data.common.taxonomy.request.TaxonomySearchRequest;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;

@Service
@Import(TaxonomySolrQueryConfig.class)
public class TaxonomyService extends BasicSearchService<TaxonomyDocument, TaxonomyEntry> {
    public static final String TAXONOMY_ID_FIELD = "id";
    private final SearchFieldConfig searchFieldConfig;
    private final RdfStreamer rdfStreamer;
    private final DefaultDocumentIdStream<TaxonomyDocument> documentIdStream;
    private final RequestConverter taxonomyRequestConverter;
    private final TaxonomyRepository repository;

    public TaxonomyService(
            TaxonomyRepository repository,
            TaxonomyEntryConverter converter,
            SearchFieldConfig taxonomySearchFieldConfig,
            RdfStreamer supportDataRdfStreamer,
            DefaultDocumentIdStream<TaxonomyDocument> documentIdStream,
            RequestConverter taxonomyRequestConverter) {

        super(repository, converter, taxonomyRequestConverter);
        this.searchFieldConfig = taxonomySearchFieldConfig;
        this.rdfStreamer = supportDataRdfStreamer;
        this.documentIdStream = documentIdStream;
        this.taxonomyRequestConverter = taxonomyRequestConverter;
        this.repository = repository;
    }

    public TaxonomyEntry findById(final long taxId) {
        return findByUniqueId(String.valueOf(taxId));
    }

    public QueryResult<TaxonomyEntry> search(TaxonomySearchRequest request) {
        SolrRequest solrRequest = taxonomyRequestConverter.createSearchSolrRequest(request);
        QueryResult<TaxonomyDocument> results =
                repository.searchPage(solrRequest, request.getCursor());

        Stream<TaxonomyEntry> converted = convertDocumentsToEntries(request, results);

        Set<ProblemPair> warnings = getWarnings(request.getQuery(), Set.of());
        return QueryResult.<TaxonomyEntry>builder()
                .content(converted)
                .page(results.getPage())
                .facets(results.getFacets())
                .matchedFields(results.getMatchedFields())
                .suggestions(results.getSuggestions())
                .warnings(warnings)
                .build();
    }

    @Override
    protected SearchFieldItem getIdField() {
        return this.searchFieldConfig.getSearchFieldItemByName(TAXONOMY_ID_FIELD);
    }

    @Override
    protected RdfStreamer getRdfStreamer() {
        return this.rdfStreamer;
    }

    @Override
    protected DefaultDocumentIdStream<TaxonomyDocument> getDocumentIdStream() {
        return this.documentIdStream;
    }

    public QueryResult<TaxonomyEntry> searchByIds(
            @Valid GetByTaxonIdsRequest getByTaxonIdsRequest) {
        SolrRequest solrRequest =
                taxonomyRequestConverter.createSearchIdsSolrRequest(
                        getByTaxonIdsRequest, null, null);
        QueryResult<TaxonomyDocument> results =
                repository.searchPage(solrRequest, getByTaxonIdsRequest.getCursor());

        Stream<TaxonomyEntry> converted = convertDocumentsToEntries(getByTaxonIdsRequest, results);

        Set<ProblemPair> warnings = getWarnings(getByTaxonIdsRequest.getQuery(), Set.of());
        return QueryResult.<TaxonomyEntry>builder()
                .content(converted)
                .page(results.getPage())
                .facets(results.getFacets())
                .suggestions(results.getSuggestions())
                .warnings(warnings)
                .build();
    }
}
