package org.uniprot.api.uniprotkb.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.store.StoreStreamer;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.service.StoreStreamerSearchService;
import org.uniprot.api.uniprotkb.controller.request.FieldsParser;
import org.uniprot.api.uniprotkb.controller.request.UniProtKBRequest;
import org.uniprot.api.uniprotkb.repository.search.impl.UniProtQueryBoostsConfig;
import org.uniprot.api.uniprotkb.repository.search.impl.UniProtTermsConfig;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotKBFacetConfig;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotQueryRepository;
import org.uniprot.api.uniprotkb.repository.store.UniProtKBStoreClient;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.search.SolrQueryUtil;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

@Service
@Import(UniProtQueryBoostsConfig.class)
public class UniProtEntryService
        extends StoreStreamerSearchService<UniProtDocument, UniProtKBEntry> {
    private static final String ACCESSION = "accession_id";
    private final UniProtEntryQueryResultsConverter resultsConverter;
    private final QueryBoosts queryBoosts;
    private final UniProtTermsConfig uniProtTermsConfig;
    private UniprotQueryRepository repository;
    private StoreStreamer<UniProtDocument, UniProtKBEntry> storeStreamer;
    private final SearchFieldConfig searchFieldConfig;

    public UniProtEntryService(
            UniprotQueryRepository repository,
            UniprotKBFacetConfig uniprotKBFacetConfig,
            UniProtTermsConfig uniProtTermsConfig,
            UniProtSolrSortClause uniProtSolrSortClause,
            QueryBoosts uniProtKBQueryBoosts,
            UniProtKBStoreClient entryStore,
            StoreStreamer<UniProtDocument, UniProtKBEntry> uniProtEntryStoreStreamer,
            TaxonomyService taxService) {
        super(
                repository,
                uniprotKBFacetConfig,
                uniProtSolrSortClause,
                uniProtEntryStoreStreamer,
                uniProtKBQueryBoosts);
        this.repository = repository;
        this.uniProtTermsConfig = uniProtTermsConfig;
        this.queryBoosts = uniProtKBQueryBoosts;
        this.resultsConverter = new UniProtEntryQueryResultsConverter(entryStore, taxService);
        this.storeStreamer = uniProtEntryStoreStreamer;
        this.searchFieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPROTKB);
    }

    @Override
    public QueryResult<UniProtKBEntry> search(SearchRequest request) {

        SolrRequest solrRequest = createSearchSolrRequest(request, true);

        QueryResult<UniProtDocument> results =
                repository.searchPage(solrRequest, request.getCursor());

        return resultsConverter.convertQueryResult(
                results, FieldsParser.parseForFilters(request.getFields()));
    }

    @Override
    public UniProtKBEntry findByUniqueId(String accession) {
        return findByUniqueId(accession, null);
    }

    @Override
    protected String getIdField() {
        return searchFieldConfig.getSearchFieldItemByName("accession_id").getFieldName();
    }

    @Override
    public UniProtKBEntry findByUniqueId(String accession, String fields) {
        try {
            Map<String, List<String>> filters = FieldsParser.parseForFilters(fields);
            SolrRequest solrRequest =
                    SolrRequest.builder()
                            .query(ACCESSION + ":" + accession.toUpperCase())
                            .rows(NumberUtils.INTEGER_ONE)
                            .build();
            Optional<UniProtDocument> optionalDoc = repository.getEntry(solrRequest);
            Optional<UniProtKBEntry> optionalUniProtEntry =
                    optionalDoc
                            .map(doc -> resultsConverter.convertDoc(doc, filters))
                            .orElseThrow(() -> new ResourceNotFoundException("{search.not.found}"));

            return optionalUniProtEntry.orElseThrow(
                    () -> new ResourceNotFoundException("{search.not.found}"));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            String message = "Could not get accession for: [" + accession + "]";
            throw new ServiceException(message, e);
        }
    }

    public Stream<String> streamRDF(SearchRequest searchRequest) {
        SolrRequest solrRequest = createDownloadSolrRequest(searchRequest);
        return this.storeStreamer.idsToRDFStoreStream(solrRequest);
    }

    @Override
    protected SolrRequest createSolrRequest(SearchRequest request, boolean includeFacets) {

        UniProtKBRequest uniProtRequest = (UniProtKBRequest) request;
        // fill the common params from the basic service class
        SolrRequest solrRequest = super.createSolrRequest(uniProtRequest, includeFacets);

        // uniprotkb related stuff
        solrRequest.setQueryBoosts(queryBoosts);

        if (needsToFilterIsoform(uniProtRequest)) {
            List<String> queries = new ArrayList<>(solrRequest.getFilterQueries());
            queries.add(
                    searchFieldConfig.getSearchFieldItemByName("is_isoform").getFieldName()
                            + ":"
                            + false);
            solrRequest.setFilterQueries(queries);
        }

        if (uniProtRequest.isShowMatchedFields()) {
            solrRequest.setTermQuery(uniProtRequest.getQuery());
            List<String> termFields = new ArrayList<>(uniProtTermsConfig.getFields());
            solrRequest.setTermFields(termFields);
        }

        return solrRequest;
    }

    /**
     * This method verify if we need to add isoform filter query to remove isoform entries
     *
     * <p>if does not have id fields (we can not filter isoforms when querying for IDS) AND has
     * includeIsoform params in the request URL Then we analyze the includeIsoform request
     * parameter. IMPORTANT: Implementing this way, query search has precedence over isoform request
     * parameter
     *
     * @return true if we need to add isoform filter query
     */
    private boolean needsToFilterIsoform(UniProtKBRequest request) {
        boolean hasIdFieldTerms =
                SolrQueryUtil.hasFieldTerms(
                        request.getQuery(),
                        searchFieldConfig.getSearchFieldItemByName("accession_id").getFieldName(),
                        searchFieldConfig.getSearchFieldItemByName("mnemonic").getFieldName(),
                        searchFieldConfig.getSearchFieldItemByName("is_isoform").getFieldName());

        if (!hasIdFieldTerms) {
            return !request.isIncludeIsoform();
        } else {
            return false;
        }
    }
}
