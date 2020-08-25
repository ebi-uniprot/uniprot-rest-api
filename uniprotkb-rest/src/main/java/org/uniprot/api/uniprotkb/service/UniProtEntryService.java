package org.uniprot.api.uniprotkb.service;

import java.util.*;
import java.util.stream.Stream;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.facet.FacetTupleStreamConverter;
import org.uniprot.api.common.repository.search.facet.SolrStreamFacetResponse;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.solrstream.SolrStreamFacetRequest;
import org.uniprot.api.common.repository.store.StoreStreamer;
import org.uniprot.api.rest.output.converter.OutputFieldsParser;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.service.DefaultSearchQueryOptimiser;
import org.uniprot.api.rest.service.StoreStreamerSearchService;
import org.uniprot.api.rest.service.query.QueryProcessor;
import org.uniprot.api.rest.service.query.UniProtQueryProcessor;
import org.uniprot.api.rest.service.query.processor.UniProtQueryNodeProcessorPipeline;
import org.uniprot.api.uniprotkb.controller.request.GetByAccessionsRequest;
import org.uniprot.api.uniprotkb.controller.request.UniProtKBSearchRequest;
import org.uniprot.api.uniprotkb.controller.request.UniProtKBStreamRequest;
import org.uniprot.api.uniprotkb.repository.search.impl.UniProtQueryBoostsConfig;
import org.uniprot.api.uniprotkb.repository.search.impl.UniProtTermsConfig;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotKBFacetConfig;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotQueryRepository;
import org.uniprot.api.uniprotkb.repository.store.UniProtKBStoreClient;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.util.Utils;
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
@Import(UniProtQueryBoostsConfig.class)
public class UniProtEntryService
        extends StoreStreamerSearchService<UniProtDocument, UniProtKBEntry> {
    private static final String ACCESSION = "accession_id";
    private final UniProtEntryQueryResultsConverter resultsConverter;
    private final QueryBoosts uniProtKBqueryBoosts;
    private final UniProtTermsConfig uniProtTermsConfig;
    private final UniprotQueryRepository repository;
    private final StoreStreamer<UniProtKBEntry> storeStreamer;
    private final SearchFieldConfig searchFieldConfig;
    private final ReturnFieldConfig returnFieldConfig;
    private final FacetTupleStreamTemplate facetTupleStreamTemplate;
    private final FacetTupleStreamConverter facetTupleStreamConverter;
    private final QueryProcessor queryProcessor;

    public UniProtEntryService(
            UniprotQueryRepository repository,
            UniprotKBFacetConfig uniprotKBFacetConfig,
            UniProtTermsConfig uniProtTermsConfig,
            UniProtSolrSortClause uniProtSolrSortClause,
            QueryBoosts uniProtKBQueryBoosts,
            UniProtKBStoreClient entryStore,
            StoreStreamer<UniProtKBEntry> uniProtEntryStoreStreamer,
            TaxonomyService taxService,
            FacetTupleStreamTemplate facetTupleStreamTemplate) {
        super(
                repository,
                uniprotKBFacetConfig,
                uniProtSolrSortClause,
                uniProtEntryStoreStreamer,
                uniProtKBQueryBoosts);
        this.repository = repository;
        this.uniProtTermsConfig = uniProtTermsConfig;
        this.uniProtKBqueryBoosts = uniProtKBQueryBoosts;
        this.resultsConverter = new UniProtEntryQueryResultsConverter(entryStore, taxService);
        this.storeStreamer = uniProtEntryStoreStreamer;
        this.searchFieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPROTKB);
        this.returnFieldConfig =
                ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIPROTKB);
        this.queryProcessor =
                UniProtQueryProcessor.builder()
                        .queryProcessorPipeline(
                                new UniProtQueryNodeProcessorPipeline(
                                        getDefaultSearchOptimisedFieldItems()))
                        .build();        this.facetTupleStreamConverter = new FacetTupleStreamConverter(uniprotKBFacetConfig);
        this.facetTupleStreamTemplate = facetTupleStreamTemplate;
    }

    @Override
    public QueryResult<UniProtKBEntry> search(SearchRequest request) {

        SolrRequest solrRequest = createSearchSolrRequest(request, true);

        QueryResult<UniProtDocument> results =
                repository.searchPage(solrRequest, request.getCursor());
        List<ReturnField> fields = OutputFieldsParser.parse(request.getFields(), returnFieldConfig);
        return resultsConverter.convertQueryResult(results, fields);
    }

    public QueryResult<UniProtKBEntry> getByAccessions(GetByAccessionsRequest accessionsRequest) {
        SolrStreamFacetResponse solrStreamResponse = searchBySolrStream(accessionsRequest);
        // use the accessions returned by solr stream if facetFilter is passed
        // otherwise use the passed accessions
        List<String> accessions =
                Utils.notNullNotEmpty(accessionsRequest.getFacetFilter())
                        ? solrStreamResponse.getAccessions()
                        : accessionsRequest.getAccessionsList();
        // default page size to number of accessions passed
        int pageSize =
                Objects.isNull(accessionsRequest.getSize())
                        ? accessions.size()
                        : accessionsRequest.getSize();

        // compute the cursor and get subset of accessions as per cursor
        CursorPage cursorPage =
                CursorPage.of(accessionsRequest.getCursor(), pageSize, accessions.size());

        List<String> accessionsInPage =
                accessions.subList(
                        cursorPage.getOffset().intValue(), CursorPage.getNextOffset(cursorPage));

        // get n accessions from store
        Stream<UniProtKBEntry> entries = this.storeStreamer.streamEntries(accessionsInPage);

        // facets may be set when facetList is passed but that should not be returned with cursor
        List<Facet> facets = solrStreamResponse.getFacets();
        if (Objects.nonNull(accessionsRequest.getCursor())) {
            facets = null; // do not return facet in case of next page and facetFilter
        }

        return QueryResult.of(entries, cursorPage, facets, null);
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

    public Stream<String> streamRDF(UniProtKBStreamRequest streamRequest) {
        SolrRequest solrRequest =
                createSolrRequestBuilder(streamRequest, solrSortClause, uniProtKBqueryBoosts)
                        .build();
        return this.storeStreamer.idsToRDFStoreStream(solrRequest);
    }

    @Override
    protected QueryProcessor getQueryProcessor() {
        return queryProcessor;
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
    protected SolrRequest createSolrRequest(SearchRequest request, boolean includeFacets) {

        UniProtKBSearchRequest uniProtRequest = (UniProtKBSearchRequest) request;
        // fill the common params from the basic service class
        SolrRequest solrRequest = super.createSolrRequest(uniProtRequest, includeFacets);

        // uniprotkb related stuff
        solrRequest.setQueryBoosts(uniProtKBqueryBoosts);

        if (needsToFilterIsoform(uniProtRequest.getQuery(), uniProtRequest.isIncludeIsoform())) {
            addIsoformFilter(solrRequest);
        }

        if (uniProtRequest.isShowMatchedFields()) {
            solrRequest.setTermQuery(uniProtRequest.getQuery());
            List<String> termFields = new ArrayList<>(uniProtTermsConfig.getFields());
            solrRequest.setTermFields(termFields);
        }

        return solrRequest;
    }

    private void addIsoformFilter(SolrRequest solrRequest) {
        List<String> queries = new ArrayList<>(solrRequest.getFilterQueries());
        queries.add(
                searchFieldConfig.getSearchFieldItemByName("is_isoform").getFieldName()
                        + ":"
                        + false);
        solrRequest.setFilterQueries(queries);
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
    private boolean needsToFilterIsoform(String query, boolean isIncludeIsoform) {
        boolean hasIdFieldTerms =
                SolrQueryUtil.hasFieldTerms(
                        query,
                        searchFieldConfig.getSearchFieldItemByName(ACCESSION).getFieldName(),
                        searchFieldConfig.getSearchFieldItemByName("id").getFieldName(),
                        searchFieldConfig.getSearchFieldItemByName("is_isoform").getFieldName());

        if (!hasIdFieldTerms) {
            return !isIncludeIsoform;
        } else {
            return false;
        }
    }

    private SolrStreamFacetResponse searchBySolrStream(GetByAccessionsRequest accessionsRequest) {
        SolrStreamFacetRequest solrStreamRequest = createSolrStreamRequest(accessionsRequest);
        SolrStreamFacetResponse solrStreamResponse = new SolrStreamFacetResponse();

        if (solrStreamNeeded(accessionsRequest)) {
            TupleStream tupleStream = this.facetTupleStreamTemplate.create(solrStreamRequest);
            solrStreamResponse = this.facetTupleStreamConverter.convert(tupleStream);
        }

        return solrStreamResponse;
    }

    private SolrStreamFacetRequest createSolrStreamRequest(
            GetByAccessionsRequest accessionsRequest) {
        SolrStreamFacetRequest.SolrStreamFacetRequestBuilder solrRequestBuilder =
                SolrStreamFacetRequest.builder();

        List<String> accessions = accessionsRequest.getAccessionsList();
        List<String> facets = accessionsRequest.getFacetList();
        // construct the query for tuple stream
        StringBuilder qb = new StringBuilder();
        qb.append("accession_id:(").append(String.join(" OR ", accessions)).append(")");
        // append the facet filter query in the accession query
        if (Utils.notNullNotEmpty(accessionsRequest.getFacetFilter())) {
            qb.append(" AND (").append(accessionsRequest.getFacetFilter()).append(")");
            solrRequestBuilder.searchAccession(Boolean.TRUE);
        }

        return solrRequestBuilder.query(qb.toString()).facets(facets).build();
    }

    private boolean solrStreamNeeded(GetByAccessionsRequest accessionsRequest) {
        return (Utils.nullOrEmpty(accessionsRequest.getCursor())
                        && Utils.notNullNotEmpty(accessionsRequest.getFacetList())
                        && !accessionsRequest.isDownload())
                || Utils.notNullNotEmpty(accessionsRequest.getFacetFilter());
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems() {
        return Collections.singletonList(getIdField());
    }
}
