package org.uniprot.api.uniprotkb.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ImportantMessageServiceException;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.rest.output.converter.OutputFieldsParser;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.respository.facet.impl.UniProtKBFacetConfig;
import org.uniprot.api.rest.service.StoreStreamerSearchService;
import org.uniprot.api.rest.service.query.config.UniProtSolrQueryConfig;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.uniprotkb.controller.request.UniProtKBSearchRequest;
import org.uniprot.api.uniprotkb.controller.request.UniProtKBStreamRequest;
import org.uniprot.api.uniprotkb.repository.search.impl.UniProtTermsConfig;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotQueryRepository;
import org.uniprot.api.uniprotkb.repository.store.UniProtKBStoreClient;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.config.returnfield.model.ReturnField;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.SolrQueryUtil;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

@Service
@Import(UniProtSolrQueryConfig.class)
public class UniProtEntryService
        extends StoreStreamerSearchService<UniProtDocument, UniProtKBEntry> {
    public static final String ACCESSION = "accession_id";
    public static final String PROTEIN_ID = "id";
    public static final String IS_ISOFORM = "is_isoform";
    private final UniProtEntryQueryResultsConverter resultsConverter;
    private final SolrQueryConfig solrQueryConfig;
    private final UniProtQueryProcessorConfig uniProtQueryProcessorConfig;
    private final UniProtTermsConfig uniProtTermsConfig;
    private final UniprotQueryRepository repository;
    private final SearchFieldConfig searchFieldConfig;
    private final ReturnFieldConfig returnFieldConfig;
    private final RDFStreamer uniProtRDFStreamer;

    public UniProtEntryService(
            UniprotQueryRepository repository,
            UniProtKBFacetConfig uniprotKBFacetConfig,
            UniProtTermsConfig uniProtTermsConfig,
            UniProtSolrSortClause uniProtSolrSortClause,
            SolrQueryConfig uniProtKBSolrQueryConf,
            UniProtKBStoreClient entryStore,
            StoreStreamer<UniProtKBEntry> uniProtEntryStoreStreamer,
            TaxonomyService taxService,
            FacetTupleStreamTemplate facetTupleStreamTemplate,
            UniProtQueryProcessorConfig uniProtKBQueryProcessorConfig,
            SearchFieldConfig uniProtKBSearchFieldConfig,
            RDFStreamer uniProtRDFStreamer) {
        super(
                repository,
                uniprotKBFacetConfig,
                uniProtSolrSortClause,
                uniProtEntryStoreStreamer,
                uniProtKBSolrQueryConf,
                facetTupleStreamTemplate);
        this.repository = repository;
        this.uniProtTermsConfig = uniProtTermsConfig;
        this.solrQueryConfig = uniProtKBSolrQueryConf;
        this.uniProtQueryProcessorConfig = uniProtKBQueryProcessorConfig;
        this.resultsConverter = new UniProtEntryQueryResultsConverter(entryStore, taxService);
        this.searchFieldConfig = uniProtKBSearchFieldConfig;
        this.returnFieldConfig =
                ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIPROTKB);
        this.uniProtRDFStreamer = uniProtRDFStreamer;
    }

    @Override
    public QueryResult<UniProtKBEntry> search(SearchRequest request) {

        SolrRequest solrRequest = createSearchSolrRequest(request);

        QueryResult<UniProtDocument> results =
                repository.searchPage(solrRequest, request.getCursor());
        List<ReturnField> fields = OutputFieldsParser.parse(request.getFields(), returnFieldConfig);
        return resultsConverter.convertQueryResult(results, fields);
    }

    @Override
    public UniProtKBEntry findByUniqueId(String accession) {
        return findByUniqueId(accession, null);
    }

    @Override
    protected SearchFieldItem getIdField() {
        return searchFieldConfig.getSearchFieldItemByName(ACCESSION);
    }

    @Override
    protected UniProtQueryProcessorConfig getQueryProcessorConfig() {
        return uniProtQueryProcessorConfig;
    }

    @Override
    public UniProtKBEntry findByUniqueId(String accession, String fields) {
        try {
            List<ReturnField> fieldList = OutputFieldsParser.parse(fields, returnFieldConfig);
            SolrRequest solrRequest =
                    SolrRequest.builder()
                            .query(ACCESSION + ":" + accession.toUpperCase())
                            .rows(NumberUtils.INTEGER_ONE)
                            .build();
            Optional<UniProtDocument> optionalDoc = repository.getEntry(solrRequest);
            Optional<UniProtKBEntry> optionalUniProtEntry =
                    optionalDoc
                            .map(doc -> resultsConverter.convertDoc(doc, fieldList))
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

    public String findAccessionByProteinId(String proteinId) {
        try {
            SolrRequest solrRequest =
                    SolrRequest.builder()
                            .query(PROTEIN_ID + ":" + proteinId.toUpperCase() + " AND  " + IS_ISOFORM + ":false")
                            .sorts(solrSortClause.getSort(null))
                            .rows(NumberUtils.INTEGER_TWO)
                            .build();
            QueryResult<UniProtDocument> queryResult = repository.searchPage(solrRequest, null);
            if(queryResult.getPage().getTotalElements() > 0){
                List<UniProtDocument> docResult = queryResult.getContent().collect(Collectors.toList());
                if(docResult.size() > 1){
                    docResult = docResult.stream().filter(doc -> doc.active != null && doc.active).collect(Collectors.toList());
                }
                if(docResult.size() > 1){
                    throw new ImportantMessageServiceException("Multiple accessions found for id: "+proteinId);
                } else {
                    return docResult.get(0).accession;
                }
            } else {
                throw new ResourceNotFoundException("{search.not.found}");
            }
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            String message = "Could not get protein id for: [" + proteinId + "]";
            throw new ServiceException(message, e);
        }
    }

    public Stream<String> streamRDF(UniProtKBStreamRequest streamRequest) {
        SolrRequest solrRequest =
                createSolrRequestBuilder(streamRequest, solrSortClause, solrQueryConfig).build();
        return this.uniProtRDFStreamer.idsToRDFStoreStream(solrRequest);
    }

    @Override
    public SolrRequest createDownloadSolrRequest(StreamRequest request) {
        UniProtKBStreamRequest uniProtRequest = (UniProtKBStreamRequest) request;
        SolrRequest solrRequest = super.createDownloadSolrRequest(request);
        if (needsToFilterIsoform(uniProtRequest.getQuery(), uniProtRequest.isIncludeIsoform())) {
            addIsoformFilter(solrRequest);
        }
        return solrRequest;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIPROTKB;
    }

    @Override
    protected String getSolrIdField() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPROTKB)
                .getSearchFieldItemByName(ACCESSION)
                .getFieldName();
    }

    @Override
    public SolrRequest createSearchSolrRequest(SearchRequest request) {

        UniProtKBSearchRequest uniProtRequest = (UniProtKBSearchRequest) request;

        if (isSearchAll(uniProtRequest)) {
            uniProtRequest.setQuery(getQueryFieldName("active") + ":" + true);
        } else if (needToAddActiveFilter(uniProtRequest)) {
            uniProtRequest.setQuery(
                    uniProtRequest.getQuery() + " AND " + getQueryFieldName("active") + ":" + true);
        }

        // fill the common params from the basic service class
        SolrRequest solrRequest = super.createSearchSolrRequest(uniProtRequest);

        // uniprotkb related stuff
        solrRequest.setQueryConfig(solrQueryConfig);

        if (needsToFilterIsoform(solrRequest.getQuery(), uniProtRequest.isIncludeIsoform())) {
            addIsoformFilter(solrRequest);
        }

        if (uniProtRequest.getShowSingleTermMatchedFields()) {
            solrRequest.setTermQuery(uniProtRequest.getQuery());
            List<String> termFields = new ArrayList<>(uniProtTermsConfig.getFields());
            solrRequest.setTermFields(termFields);
        }

        return solrRequest;
    }

    @Override
    protected RDFStreamer getRDFStreamer() {
        return this.uniProtRDFStreamer;
    }

    private void addIsoformFilter(SolrRequest solrRequest) {
        List<String> queries = new ArrayList<>(solrRequest.getFilterQueries());
        queries.add(getQueryFieldName("is_isoform") + ":" + false);
        solrRequest.setFilterQueries(queries);
    }

    /**
     * This method verify if we need to add isoform filter query to remove isoform entries
     *
     * <p>if the query does not have accession_id field (we should not filter isoforms when querying
     * for accession_id) AND has includeIsoform params in the request URL Then we analyze the
     * includeIsoform request parameter. IMPORTANT: Implementing this way, query search has
     * precedence over isoform request parameter
     *
     * @return true if we need to add isoform filter query
     */
    private boolean needsToFilterIsoform(String query, boolean isIncludeIsoform) {
        boolean hasIdFieldTerms =
                SolrQueryUtil.hasFieldTerms(
                        query, getQueryFieldName(ACCESSION), getQueryFieldName("is_isoform"));

        List<String> accessionValues =
                SolrQueryUtil.getTermValuesWithWhitespaceAnalyzer(query, "accession");
        boolean hasIsoforms =
                !accessionValues.isEmpty()
                        && accessionValues.stream().allMatch(acc -> acc.contains("-"));

        if (!hasIdFieldTerms && !hasIsoforms) {
            return !isIncludeIsoform;
        } else {
            return false;
        }
    }

    private boolean isSearchAll(UniProtKBSearchRequest uniProtRequest) {
        return "*".equals(uniProtRequest.getQuery().strip())
                || "(*)".equals(uniProtRequest.getQuery().strip())
                || "*:*".equals(uniProtRequest.getQuery().strip())
                || "(*:*)".equals(uniProtRequest.getQuery().strip());
    }

    private boolean needToAddActiveFilter(UniProtKBSearchRequest uniProtRequest) {
        return SolrQueryUtil.hasNegativeTerm(uniProtRequest.getQuery());
    }

    private String getQueryFieldName(String active) {
        return searchFieldConfig.getSearchFieldItemByName(active).getFieldName();
    }
}
