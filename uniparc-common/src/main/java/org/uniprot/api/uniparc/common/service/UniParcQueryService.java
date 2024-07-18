package org.uniprot.api.uniparc.common.service;

import static org.uniprot.store.search.field.validator.FieldRegexConstants.UNIPARC_UPI_REGEX;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.document.TupleStreamDocumentIdStream;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.rest.request.BasicRequest;
import org.uniprot.api.rest.respository.facet.impl.UniParcFacetConfig;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.api.rest.service.StoreStreamerSearchService;
import org.uniprot.api.rest.service.query.config.UniParcSolrQueryConfig;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.uniparc.common.repository.search.UniParcQueryRepository;
import org.uniprot.api.uniparc.common.repository.store.light.UniParcLightStoreClient;
import org.uniprot.api.uniparc.common.response.converter.UniParcQueryResultConverter;
import org.uniprot.api.uniparc.common.service.filter.UniParcCrossReferenceTaxonomyFilter;
import org.uniprot.api.uniparc.common.service.filter.UniParcDatabaseFilter;
import org.uniprot.api.uniparc.common.service.filter.UniParcDatabaseStatusFilter;
import org.uniprot.api.uniparc.common.service.light.UniParcCrossReferenceService;
import org.uniprot.api.uniparc.common.service.request.*;
import org.uniprot.api.uniparc.common.service.sort.UniParcSortClause;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.uniparc.impl.UniParcEntryBuilder;
import org.uniprot.core.util.MessageDigestUtil;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.uniparc.UniParcDocument;

/**
 * @author jluo
 * @date: 21 Jun 2019
 */
@Service
@Import(UniParcSolrQueryConfig.class)
public class UniParcQueryService extends StoreStreamerSearchService<UniParcDocument, UniParcEntry> {
    public static final String UNIPARC_ID_FIELD = "upi";
    private static final String ACCESSION_FIELD = "uniprotkb";
    public static final String CHECKSUM_STR = "checksum";
    private static final String COMMA_STR = ",";
    private final UniProtQueryProcessorConfig uniParcQueryProcessorConfig;
    private final SearchFieldConfig searchFieldConfig;
    private final UniParcQueryRepository repository;
    private final UniParcQueryResultConverter entryConverter;
    private final SolrQueryConfig solrQueryConfig;
    private final RdfStreamer rdfStreamer;

    private static final Pattern UNIPARC_UPI_REGEX_PATTERN = Pattern.compile(UNIPARC_UPI_REGEX);
    private final UniParcLightStoreClient uniParcLightStoreClient;
    private final UniParcCrossReferenceService uniParcCrossReferenceService;

    @Autowired
    public UniParcQueryService(
            UniParcQueryRepository repository,
            UniParcFacetConfig facetConfig,
            UniParcSortClause solrSortClause,
            UniParcQueryResultConverter uniParcQueryResultConverter,
            StoreStreamer<UniParcEntry> storeStreamer,
            SolrQueryConfig uniParcSolrQueryConf,
            UniProtQueryProcessorConfig uniParcQueryProcessorConfig,
            SearchFieldConfig uniParcSearchFieldConfig,
            RdfStreamer uniParcRdfStreamer,
            FacetTupleStreamTemplate uniParcFacetTupleStreamTemplate,
            TupleStreamDocumentIdStream uniParcTupleStreamDocumentIdStream,
            UniParcLightStoreClient uniParcLightStoreClient,
            UniParcCrossReferenceService uniParcCrossReferenceService) {

        super(
                repository,
                uniParcQueryResultConverter,
                solrSortClause,
                facetConfig,
                storeStreamer,
                uniParcSolrQueryConf,
                uniParcFacetTupleStreamTemplate,
                uniParcTupleStreamDocumentIdStream);
        this.uniParcQueryProcessorConfig = uniParcQueryProcessorConfig;
        this.searchFieldConfig = uniParcSearchFieldConfig;
        this.repository = repository;
        this.entryConverter = uniParcQueryResultConverter;
        this.solrQueryConfig = uniParcSolrQueryConf;
        this.rdfStreamer = uniParcRdfStreamer;
        this.uniParcLightStoreClient = uniParcLightStoreClient;
        this.uniParcCrossReferenceService = uniParcCrossReferenceService;
    }

    public UniParcEntry getByUniParcId(UniParcGetByUniParcIdRequest uniParcIdRequest) {
        UniParcEntry uniParcEntry = createUniParcEntry(uniParcIdRequest.getUpi());

        return filterUniParcStream(Stream.of(uniParcEntry), uniParcIdRequest)
                .findFirst()
                .orElse(null);
    }

    public UniParcEntry getByUniProtAccession(UniParcGetByAccessionRequest getByAccessionRequest) {
        String uniParcId = searchUniParcId(ACCESSION_FIELD, getByAccessionRequest.getAccession());
        UniParcEntry uniParcEntry = createUniParcEntry(uniParcId);

        return filterUniParcStream(Stream.of(uniParcEntry), getByAccessionRequest)
                .findFirst()
                .orElse(null);
    }

    public UniParcEntry getBySequence(UniParcSequenceRequest sequenceRequest) {

        String md5Value = MessageDigestUtil.getMD5(sequenceRequest.getSequence());
        String uniParcId = searchUniParcId(CHECKSUM_STR, md5Value);
        UniParcEntry uniParcEntry = createUniParcEntry(uniParcId);

        return filterUniParcStream(Stream.of(uniParcEntry), sequenceRequest)
                .findFirst()
                .orElse(null);
    }

    public QueryResult<UniParcEntry> searchByFieldId(
            UniParcGetByIdPageSearchRequest searchRequest) {
        // search uniparc entries from solr
        SolrRequest solrRequest = createSearchSolrRequest(searchRequest);
        QueryResult<UniParcDocument> results =
                repository.searchPage(solrRequest, searchRequest.getCursor());

        // convert solr docs to entries
        Stream<UniParcEntry> converted =
                results.getContent().map(entryConverter).filter(Objects::nonNull);
        // filter the entries
        Stream<UniParcEntry> filtered = filterUniParcStream(converted, searchRequest);
        return QueryResult.<UniParcEntry>builder()
                .content(filtered)
                .page(results.getPage())
                .facets(results.getFacets())
                .build();
    }

    public Stream<String> streamRdf(
            UniParcStreamRequest streamRequest, String dataType, String format) {
        SolrRequest solrRequest =
                createSolrRequestBuilder(streamRequest, solrSortClause, solrQueryConfig).build();
        List<String> entryIds = solrIdStreamer.fetchIds(solrRequest).collect(Collectors.toList());
        return rdfStreamer.stream(entryIds.stream(), dataType, format);
    }

    @Override
    protected SearchFieldItem getIdField() {
        return this.searchFieldConfig.getSearchFieldItemByName(UNIPARC_ID_FIELD);
    }

    @Override
    protected UniProtQueryProcessorConfig getQueryProcessorConfig() {
        return uniParcQueryProcessorConfig;
    }

    @Override
    public UniParcEntry findByUniqueId(String uniqueId, String filters) {
        return findByUniqueId(uniqueId);
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIPARC;
    }

    @Override
    protected String getSolrIdField() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPARC)
                .getSearchFieldItemByName(UNIPARC_ID_FIELD)
                .getFieldName();
    }

    public UniParcEntry getUniParcBestGuess(UniParcBestGuessRequest request) {
        UniParcStreamRequest streamRequest = new UniParcStreamRequest();
        streamRequest.setQuery(request.getQuery());
        streamRequest.setFields(request.getFields());

        Stream<UniParcEntry> streamResult = stream(streamRequest);

        BestGuessAnalyser analyser = new BestGuessAnalyser(searchFieldConfig);
        return analyser.analyseBestGuess(streamResult, request);
    }

    public QueryResult<UniParcCrossReference> getDatabasesByUniParcId(
            String upi, UniParcDatabasesRequest request) {
        UniParcEntry uniParcEntry = getEntity(UNIPARC_ID_FIELD, upi);
        UniParcEntry filteredUniParcEntry =
                filterUniParcStream(Stream.of(uniParcEntry), request)
                        .findFirst()
                        .orElseThrow(() -> new ServiceException("Unable to filter UniParc entry"));

        List<UniParcCrossReference> databases = filteredUniParcEntry.getUniParcCrossReferences();
        int pageSize = Objects.isNull(request.getSize()) ? getDefaultPageSize() : request.getSize();
        CursorPage cursorPage = CursorPage.of(request.getCursor(), pageSize, databases.size());
        List<UniParcCrossReference> entries =
                databases.subList(
                        cursorPage.getOffset().intValue(), CursorPage.getNextOffset(cursorPage));
        return QueryResult.<UniParcCrossReference>builder()
                .content(entries.stream())
                .page(cursorPage)
                .build();
    }

    @Override
    protected RdfStreamer getRdfStreamer() {
        return this.rdfStreamer;
    }

    @Override
    protected UniParcEntry mapToThinEntry(String uniParcId) {
        UniParcEntryBuilder builder = new UniParcEntryBuilder();
        builder.uniParcId(uniParcId);
        return builder.build();
    }

    @Override
    protected SolrRequest.SolrRequestBuilder createSolrRequestBuilder(
            BasicRequest request,
            AbstractSolrSortClause solrSortClause,
            SolrQueryConfig queryBoosts) {
        if (request instanceof UniParcBasicRequest uniParcBasicRequest) {
            String cleanQuery =
                    CLEAN_QUERY_REGEX.matcher(request.getQuery().strip()).replaceAll("");
            if (UNIPARC_UPI_REGEX_PATTERN.matcher(cleanQuery.toUpperCase()).matches()) {
                uniParcBasicRequest.setQuery(cleanQuery.toUpperCase());
            }
            return super.createSolrRequestBuilder(uniParcBasicRequest, solrSortClause, queryBoosts);
        }
        return super.createSolrRequestBuilder(request, solrSortClause, queryBoosts);
    }

    private UniParcEntry createUniParcEntry(String uniParcId) {
        Optional<UniParcEntryLight> optLightEntry =
                this.uniParcLightStoreClient.getEntry(uniParcId);
        if (optLightEntry.isEmpty()) {
            throw new ResourceNotFoundException("Unable to find UniParc by id " + uniParcId);
        }
        UniParcEntryBuilder builder = new UniParcEntryBuilder();
        builder.uniParcId(uniParcId).sequence(optLightEntry.get().getSequence());
        builder.sequenceFeaturesSet(optLightEntry.get().getSequenceFeatures());
        // populate cross-references from its own store
        List<UniParcCrossReference> crossReferences =
                this.uniParcCrossReferenceService
                        .getCrossReferences(optLightEntry.get().getUniParcCrossReferences())
                        .toList();
        builder.uniParcCrossReferencesSet(crossReferences);
        return builder.build();
    }

    private Stream<UniParcEntry> filterUniParcStream(
            Stream<UniParcEntry> uniParcEntryStream, UniParcGetByIdRequest request) {
        // convert comma separated values to list
        List<String> databases = csvToList(request.getDbTypes());
        List<String> toxonomyIds = csvToList(request.getTaxonIds());
        // converters
        UniParcDatabaseFilter dbFilter = new UniParcDatabaseFilter();
        UniParcCrossReferenceTaxonomyFilter taxonFilter = new UniParcCrossReferenceTaxonomyFilter();
        UniParcDatabaseStatusFilter statusFilter = new UniParcDatabaseStatusFilter();

        // filter the results
        return uniParcEntryStream
                .map(uniParcEntry -> dbFilter.apply(uniParcEntry, databases))
                .map(uniParcEntry -> taxonFilter.apply(uniParcEntry, toxonomyIds))
                .map(uniParcEntry -> statusFilter.apply(uniParcEntry, request.getActive()));
    }

    private List<String> csvToList(String csv) {
        List<String> list = new ArrayList<>();
        if (Utils.notNullNotEmpty(csv)) {
            list =
                    Arrays.stream(csv.split(COMMA_STR))
                            .map(String::trim)
                            .map(String::toLowerCase)
                            .collect(Collectors.toList());
        }
        return list;
    }

    private String searchUniParcId(String idField, String value) {
        try {
            String query = idField + ":" + value;
            SolrRequest solrRequest =
                    SolrRequest.builder().query(query).rows(NumberUtils.INTEGER_ONE).build();
            UniParcDocument document =
                    repository
                            .getEntry(solrRequest)
                            .orElseThrow(() -> new ResourceNotFoundException("{search.not.found}"));

            return document.getUpi();
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            String message = "Could not get entity for id: [" + value + "]";
            throw new ServiceException(message, e);
        }
    }
}
