package org.uniprot.api.uniprotkb.service;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.common.params.FacetParams;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ImportantMessageServiceException;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.document.TupleStreamDocumentIdStream;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageService;
import org.uniprot.api.rest.output.converter.OutputFieldsParser;
import org.uniprot.api.rest.request.*;
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
import org.uniprot.core.uniprotkb.UniProtKBEntryType;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.config.returnfield.model.ReturnField;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.SolrQueryUtil;
import org.uniprot.store.search.document.Document;
import org.uniprot.store.search.document.uniprot.UniProtDocument;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.uniprot.api.common.repository.search.SolrQueryConverter.DEF_TYPE;
import static org.uniprot.api.common.repository.search.SolrQueryConverter.QUERY_FIELDS;
import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE_VALUE;

@Service
@Import(UniProtSolrQueryConfig.class)
public class UniProtEntryService
        extends StoreStreamerSearchService<UniProtDocument, UniProtKBEntry> {
    public static final String ACCESSION_ID = "accession_id";
    public static final String ACCESSION = "accession";
    public static final String PROTEIN_ID = "id";
    public static final String IS_ISOFORM = "is_isoform";
    public static final String CANONICAL_ISOFORM = "-1";
    private final UniProtEntryQueryResultsConverter resultsConverter;
    private final SolrQueryConfig solrQueryConfig;
    private final UniProtQueryProcessorConfig uniProtQueryProcessorConfig;
    private final UniProtTermsConfig uniProtTermsConfig;
    private final UniprotQueryRepository repository;
    private final SearchFieldConfig searchFieldConfig;
    private final ReturnFieldConfig returnFieldConfig;
    private final RdfStreamer rdfStreamer;

    private static final Pattern ACCESSION_REGEX_ISOFORM =
            Pattern.compile(FieldRegexConstants.UNIPROTKB_ACCESSION_REGEX);

    private static final Pattern CLEAN_QUERY_REGEX =
            Pattern.compile(FieldRegexConstants.CLEAN_QUERY_REGEX);

    public UniProtEntryService(
            UniprotQueryRepository repository,
            UniProtKBFacetConfig uniprotKBFacetConfig,
            UniProtTermsConfig uniProtTermsConfig,
            UniProtSolrSortClause uniProtSolrSortClause,
            SolrQueryConfig uniProtKBSolrQueryConf,
            UniProtKBStoreClient entryStore,
            StoreStreamer<UniProtKBEntry> uniProtEntryStoreStreamer,
            TaxonomyLineageService taxService,
            FacetTupleStreamTemplate facetTupleStreamTemplate,
            UniProtQueryProcessorConfig uniProtKBQueryProcessorConfig,
            SearchFieldConfig uniProtKBSearchFieldConfig,
            TupleStreamDocumentIdStream solrIdStreamer,
            RdfStreamer uniProtRdfStreamer) {
        super(
                repository,
                uniprotKBFacetConfig,
                uniProtSolrSortClause,
                uniProtEntryStoreStreamer,
                uniProtKBSolrQueryConf,
                facetTupleStreamTemplate,
                solrIdStreamer);
        this.repository = repository;
        this.uniProtTermsConfig = uniProtTermsConfig;
        this.solrQueryConfig = uniProtKBSolrQueryConf;
        this.uniProtQueryProcessorConfig = uniProtKBQueryProcessorConfig;
        this.resultsConverter = new UniProtEntryQueryResultsConverter(entryStore, taxService);
        this.searchFieldConfig = uniProtKBSearchFieldConfig;
        this.returnFieldConfig =
                ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIPROTKB);
        this.rdfStreamer = uniProtRdfStreamer;
    }

    @Override
    public QueryResult<UniProtKBEntry> search(SearchRequest request) {

        SolrRequest solrRequest = createSearchSolrRequest(request);

        QueryResult<UniProtDocument> results =
                repository.searchPage(solrRequest, request.getCursor());
        List<ReturnField> fields = OutputFieldsParser.parse(request.getFields(), returnFieldConfig);
        Set<ProblemPair> warnings =
                getWarnings(
                        request.getQuery(),
                        this.uniProtQueryProcessorConfig.getLeadingWildcardFields());
        if (LIST_MEDIA_TYPE_VALUE.equals(request.getFormat())) {
            return convertQueryResult(results, warnings);
        } else {
            return resultsConverter.convertQueryResult(results, fields, warnings);
        }
    }

    @Override
    public UniProtKBEntry findByUniqueId(String accession) {
        return findByUniqueId(accession, null);
    }

    @Override
    protected SearchFieldItem getIdField() {
        return searchFieldConfig.getSearchFieldItemByName(ACCESSION_ID);
    }

    @Override
    protected UniProtQueryProcessorConfig getQueryProcessorConfig() {
        return uniProtQueryProcessorConfig;
    }

    @Override
    public UniProtKBEntry findByUniqueId(String accession, String fields) {
        try {
            List<ReturnField> fieldList = OutputFieldsParser.parse(fields, returnFieldConfig);
            accession = accession.toUpperCase();
            SolrRequest solrRequest = buildSolrRequest(accession);
            Optional<UniProtDocument> optionalDoc = repository.getEntry(solrRequest);
            if (accession.endsWith(CANONICAL_ISOFORM) && optionalDoc.isEmpty()) {
                accession = accession.substring(0, accession.indexOf(CANONICAL_ISOFORM));
                solrRequest = buildSolrRequest(accession);
                optionalDoc = repository.getEntry(solrRequest);
            }
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

    @Override
    public Stream<UniProtKBEntry> stream(StreamRequest request) {
        SolrRequest query = createDownloadSolrRequest(request);
        if (LIST_MEDIA_TYPE_VALUE.equals(request.getFormat())) {
            return this.solrIdStreamer
                    .fetchIds(query)
                    .map(this::mapToThinEntry)
                    .filter(Objects::nonNull);
        } else {
            StoreRequest storeRequest = buildStoreRequest(request);
            return super.storeStreamer.idsToStoreStream(query, storeRequest);
        }
    }

    public List<FacetField> getFacets(String query, Map<String,String> facetFields) {
        SolrQuery solrQuery = new SolrQuery(query);
        facetFields.forEach(solrQuery::set);
        solrQuery.set(FacetParams.FACET, true);
        solrQuery.set(DEF_TYPE, "edismax");
        solrQuery.add(QUERY_FIELDS, getQueryFields(query));
        return repository.query(solrQuery).getFacetFields();
    }

    private SolrRequest buildSolrRequest(String accession) {
        return SolrRequest.builder()
                .query(ACCESSION_ID + ":" + accession)
                .rows(NumberUtils.INTEGER_ONE)
                .build();
    }

    public String findAccessionByProteinId(String proteinId) {
        try {
            SolrRequest solrRequest =
                    SolrRequest.builder()
                            .query(
                                    PROTEIN_ID
                                            + ":"
                                            + proteinId.toUpperCase()
                                            + " AND  "
                                            + IS_ISOFORM
                                            + ":false")
                            .sorts(solrSortClause.getSort(null))
                            .rows(NumberUtils.INTEGER_TWO)
                            .build();
            QueryResult<UniProtDocument> queryResult = repository.searchPage(solrRequest, null);
            if (queryResult.getPage().getTotalElements() > 0) {
                List<UniProtDocument> docResult =
                        queryResult.getContent().collect(Collectors.toList());
                if (docResult.size() > 1) {
                    docResult =
                            docResult.stream()
                                    .filter(doc -> doc.active != null && doc.active)
                                    .collect(Collectors.toList());
                }
                if (docResult.size() > 1) {
                    throw new ImportantMessageServiceException(
                            "Multiple accessions found for id: " + proteinId);
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

    public Stream<String> streamRdf(
            UniProtKBStreamRequest streamRequest, String dataType, String format) {
        SolrRequest solrRequest = createDownloadSolrRequest(streamRequest);
        List<String> entryIds = solrIdStreamer.fetchIds(solrRequest).collect(Collectors.toList());
        return rdfStreamer.stream(entryIds.stream(), dataType, format);
    }

    @Override
    public SolrRequest createDownloadSolrRequest(StreamRequest request) {
        UniProtKBStreamRequest uniProtRequest = (UniProtKBStreamRequest) request;
        SolrRequest solrRequest = super.createDownloadSolrRequest(request);
        boolean filterIsoform =
                UniProtKBRequestUtil.needsToFilterIsoform(
                        getQueryFieldName(ACCESSION_ID),
                        getQueryFieldName("is_isoform"),
                        uniProtRequest.getQuery(),
                        uniProtRequest.isIncludeIsoform());
        if (filterIsoform) {
            addIsoformFilter(solrRequest);
        }
        solrRequest.setLargeSolrStreamRestricted(uniProtRequest.isLargeSolrStreamRestricted());
        return solrRequest;
    }

    @Override
    protected Stream<UniProtKBEntry> streamEntries(
            List<String> idsInPage, IdsSearchRequest request) {
        StoreRequest storeRequest = buildStoreRequest(request);
        return this.storeStreamer.streamEntries(idsInPage, storeRequest);
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIPROTKB;
    }

    @Override
    protected String getSolrIdField() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPROTKB)
                .getSearchFieldItemByName(ACCESSION_ID)
                .getFieldName();
    }

    @Override
    protected String getTermsQueryField() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPROTKB)
                .getSearchFieldItemByName(ACCESSION)
                .getFieldName();
    }

    @Override
    public SolrRequest createSearchSolrRequest(SearchRequest request) {

        UniProtKBSearchRequest uniProtRequest = (UniProtKBSearchRequest) request;
        String cleanQuery = CLEAN_QUERY_REGEX.matcher(request.getQuery().strip()).replaceAll("");

        if (isSearchAll(uniProtRequest)) {
            uniProtRequest.setQuery(getQueryFieldName("active") + ":" + true);
        } else if (needToAddActiveFilter(uniProtRequest)) {
            uniProtRequest.setQuery(
                    uniProtRequest.getQuery() + " AND " + getQueryFieldName("active") + ":" + true);
        } else if (ACCESSION_REGEX_ISOFORM.matcher(cleanQuery.toUpperCase()).matches()) {
            uniProtRequest.setQuery(cleanQuery.toUpperCase());
        }

        // fill the common params from the basic service class
        SolrRequest solrRequest = super.createSearchSolrRequest(uniProtRequest);

        // uniprotkb related stuff
        solrRequest.setQueryConfig(solrQueryConfig);
        boolean filterIsoform =
                UniProtKBRequestUtil.needsToFilterIsoform(
                        getQueryFieldName(ACCESSION_ID),
                        getQueryFieldName("is_isoform"),
                        solrRequest.getQuery(),
                        uniProtRequest.isIncludeIsoform());
        if (filterIsoform) {
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
    protected RdfStreamer getRdfStreamer() {
        return this.rdfStreamer;
    }

    @Override
    protected UniProtKBEntry mapToThinEntry(String accession) {
        UniProtKBEntryBuilder builder =
                new UniProtKBEntryBuilder(accession, accession, UniProtKBEntryType.SWISSPROT);
        return builder.build();
    }

    private void addIsoformFilter(SolrRequest solrRequest) {
        List<String> queries = new ArrayList<>(solrRequest.getFilterQueries());
        queries.add(getQueryFieldName("is_isoform") + ":" + false);
        solrRequest.setFilterQueries(queries);
    }

    public StoreRequest buildStoreRequest(BasicRequest request) {
        List<ReturnField> fieldList =
                OutputFieldsParser.parse(request.getFields(), returnFieldConfig);
        StoreRequest.StoreRequestBuilder storeRequest = StoreRequest.builder();
        if (resultsConverter.hasLineage(fieldList)) {
            storeRequest.addLineage(true);
        }
        return storeRequest.build();
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

    private QueryResult<UniProtKBEntry> convertQueryResult(
            QueryResult<UniProtDocument> results, Set<ProblemPair> warnings) {
        Stream<UniProtKBEntry> upEntries =
                results.getContent()
                        .map(Document::getDocumentId)
                        .map(this::mapToThinEntry)
                        .filter(Objects::nonNull);
        return QueryResult.of(
                upEntries,
                results.getPage(),
                results.getFacets(),
                results.getMatchedFields(),
                null,
                results.getSuggestions(),
                warnings);
    }
}
