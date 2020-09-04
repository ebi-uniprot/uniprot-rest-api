package org.uniprot.api.uniparc.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.store.StoreStreamer;
import org.uniprot.api.rest.service.StoreStreamerSearchService;
import org.uniprot.api.rest.service.query.QueryProcessor;
import org.uniprot.api.rest.service.query.UniProtQueryProcessor;
import org.uniprot.api.uniparc.repository.UniParcFacetConfig;
import org.uniprot.api.uniparc.repository.UniParcQueryRepository;
import org.uniprot.api.uniparc.request.*;
import org.uniprot.api.uniparc.service.filter.UniParcDatabaseFilter;
import org.uniprot.api.uniparc.service.filter.UniParcDatabaseStatusFilter;
import org.uniprot.api.uniparc.service.filter.UniParcTaxonomyFilter;
import org.uniprot.core.uniparc.UniParcEntry;
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
    private static final String UNIPARC_ID_FIELD = "upi";
    private static final String ACCESSION_FIELD = "uniprotkb";
    public static final String MD5_STR = "md5";
    private static final String COMMA_STR = ",";
    private final SearchFieldConfig searchFieldConfig;
    private final UniParcQueryRepository repository;
    private final UniParcQueryResultConverter entryConverter;
    private final QueryProcessor queryProcessor;

    @Autowired
    public UniParcQueryService(
            UniParcQueryRepository repository,
            UniParcFacetConfig facetConfig,
            UniParcSortClause solrSortClause,
            UniParcQueryResultConverter uniParcQueryResultConverter,
            StoreStreamer<UniParcEntry> storeStreamer,
            SolrQueryConfig uniParcSolrQueryConf) {

        super(
                repository,
                uniParcQueryResultConverter,
                solrSortClause,
                facetConfig,
                storeStreamer,
                uniParcSolrQueryConf);
        this.searchFieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPARC);
        this.queryProcessor = new UniProtQueryProcessor(getDefaultSearchOptimisedFieldItems());
        this.repository = repository;
        this.entryConverter = uniParcQueryResultConverter;
    }

    public UniParcEntry getByUniParcId(UniParcGetByUniParcIdRequest getByUniParcIdRequest) {

        UniParcEntry uniParcEntry = getEntity(UNIPARC_ID_FIELD, getByUniParcIdRequest.getUpi());

        return filterUniParcStream(Stream.of(uniParcEntry), getByUniParcIdRequest)
                .findFirst()
                .orElse(null);
    }

    public UniParcEntry getByUniProtAccession(UniParcGetByAccessionRequest getByAccessionRequest) {

        UniParcEntry uniParcEntry =
                getEntity(ACCESSION_FIELD, getByAccessionRequest.getAccession());

        return filterUniParcStream(Stream.of(uniParcEntry), getByAccessionRequest)
                .findFirst()
                .orElse(null);
    }

    public UniParcEntry getBySequence(UniParcSequenceRequest sequenceRequest) {

        String md5Value = MessageDigestUtil.getMD5(sequenceRequest.getSequence());
        UniParcEntry uniParcEntry = getEntity(MD5_STR, md5Value);

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
        return QueryResult.of(filtered, results.getPage(), results.getFacets());
    }

    @Override
    protected SearchFieldItem getIdField() {
        return this.searchFieldConfig.getSearchFieldItemByName(UNIPARC_ID_FIELD);
    }

    @Override
    public UniParcEntry findByUniqueId(String uniqueId, String filters) {
        return findByUniqueId(uniqueId);
    }

    @Override
    protected QueryProcessor getQueryProcessor() {
        return queryProcessor;
    }

    public UniParcEntry getUniParcBestGuess(UniParcBestGuessRequest request) {
        UniParcStreamRequest streamRequest = new UniParcStreamRequest();
        streamRequest.setQuery(request.getQuery());
        streamRequest.setFields(request.getFields());

        Stream<UniParcEntry> streamResult = stream(streamRequest);

        BestGuessAnalyser analyser = new BestGuessAnalyser(searchFieldConfig);
        return analyser.analyseBestGuess(streamResult, request);
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems() {
        return Collections.singletonList(getIdField());
    }

    private Stream<UniParcEntry> filterUniParcStream(
            Stream<UniParcEntry> uniParcEntryStream, UniParcGetByIdRequest request) {
        // convert comma separated values to list
        List<String> databases = csvToList(request.getDbTypes());
        List<String> toxonomyIds = csvToList(request.getTaxonIds());
        // converters
        UniParcDatabaseFilter dbFilter = new UniParcDatabaseFilter();
        UniParcTaxonomyFilter taxonFilter = new UniParcTaxonomyFilter();
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
}
