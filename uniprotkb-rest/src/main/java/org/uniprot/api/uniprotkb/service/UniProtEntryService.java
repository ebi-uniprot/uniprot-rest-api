package org.uniprot.api.uniprotkb.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
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
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.solrstream.SolrStreamingFacetRequest;
import org.uniprot.api.common.repository.store.StoreStreamer;
import org.uniprot.api.rest.output.converter.OutputFieldsParser;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.service.StoreStreamerSearchService;
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
    private UniprotQueryRepository repository;
    private StoreStreamer<UniProtDocument, UniProtKBEntry> storeStreamer;
    private final SearchFieldConfig searchFieldConfig;
    private final ReturnFieldConfig returnFieldConfig;
    private final FacetTupleStreamTemplate facetTupleStreamTemplate;
    private final FacetTupleStreamConverter facetTupleStreamConverter;

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
        this.facetTupleStreamConverter = new FacetTupleStreamConverter(uniprotKBFacetConfig);
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
        List<String> accessions = accessionsRequest.getAccessionsList();
        // get facets using solr streaming
        List<Facet> facets = getFacets(accessionsRequest);
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
        List<UniProtKBEntry> entries =
                this.storeStreamer.streamEntries(accessionsInPage).collect(Collectors.toList());

        QueryResult<UniProtKBEntry> result = QueryResult.of(entries, cursorPage, facets, null);
        return result;
    }

    @Override
    public UniProtKBEntry findByUniqueId(String accession) {
        return findByUniqueId(accession, null);
    }

    @Override
    protected String getIdField() {
        return searchFieldConfig.getSearchFieldItemByName(ACCESSION).getFieldName();
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
    protected SolrRequest createSolrRequest(SearchRequest request, boolean includeFacets) {

        UniProtKBSearchRequest uniProtRequest = (UniProtKBSearchRequest) request;
        // fill the common params from the basic service class
        SolrRequest solrRequest = super.createSolrRequest(uniProtRequest, includeFacets);

        // uniprotkb related stuff
        solrRequest.setQueryBoosts(uniProtKBqueryBoosts);

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
    private boolean needsToFilterIsoform(UniProtKBSearchRequest request) {
        boolean hasIdFieldTerms =
                SolrQueryUtil.hasFieldTerms(
                        request.getQuery(),
                        searchFieldConfig.getSearchFieldItemByName(ACCESSION).getFieldName(),
                        searchFieldConfig.getSearchFieldItemByName("id").getFieldName(),
                        searchFieldConfig.getSearchFieldItemByName("is_isoform").getFieldName());

        if (!hasIdFieldTerms) {
            return !request.isIncludeIsoform();
        } else {
            return false;
        }
    }

    private List<Facet> getFacets(GetByAccessionsRequest accessionsRequest) {
        SolrStreamingFacetRequest solrStreamingFacetRequest =
                createSolrStreamingFacetRequest(accessionsRequest);
        List<Facet> facets = null;

        if (facetsNeeded(accessionsRequest)) {
            // create facet tuple stream and get facets
            TupleStream tupleStream =
                    this.facetTupleStreamTemplate.create(solrStreamingFacetRequest);
            facets = this.facetTupleStreamConverter.convert(tupleStream);
        }

        return facets;
    }

    private SolrStreamingFacetRequest createSolrStreamingFacetRequest(
            GetByAccessionsRequest accessionsRequest) {
        List<String> accessions = accessionsRequest.getAccessionsList();
        List<String> facets = accessionsRequest.getFacetList();
        String query = accessions.stream().collect(Collectors.joining(" "));
        query = "accession_id:(" + query + ")";
        SolrStreamingFacetRequest.SolrStreamingFacetRequestBuilder builder =
                SolrStreamingFacetRequest.builder();
        builder.query(query).facets(facets);
        return builder.build();
    }

    private boolean facetsNeeded(GetByAccessionsRequest accessionsRequest) {
        return Utils.nullOrEmpty(accessionsRequest.getCursor())
                && Utils.notNullNotEmpty(accessionsRequest.getFacetList())
                && accessionsRequest.isDownload() == false;
    }
}
