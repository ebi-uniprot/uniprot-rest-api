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
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.store.StoreStreamer;
import org.uniprot.api.rest.service.DefaultSearchQueryOptimiser;
import org.uniprot.api.rest.service.StoreStreamerSearchService;
import org.uniprot.api.uniparc.repository.UniParcFacetConfig;
import org.uniprot.api.uniparc.repository.UniParcQueryRepository;
import org.uniprot.api.uniparc.request.UniParcGetByAccessionRequest;
import org.uniprot.api.uniparc.request.UniParcGetByDbIdRequest;
import org.uniprot.api.uniparc.request.UniParcGetByIdRequest;
import org.uniprot.api.uniparc.request.UniParcGetByUpIdRequest;
import org.uniprot.api.uniparc.request.UniParcSearchRequest;
import org.uniprot.api.uniparc.service.filter.UniParcDatabaseFilter;
import org.uniprot.api.uniparc.service.filter.UniParcDatabaseIdFilter;
import org.uniprot.api.uniparc.service.filter.UniParcDatabaseStatusFilter;
import org.uniprot.api.uniparc.service.filter.UniParcTaxonomyFilter;
import org.uniprot.core.uniparc.UniParcEntry;
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
@Import(UniParcQueryBoostsConfig.class)
public class UniParcQueryService extends StoreStreamerSearchService<UniParcDocument, UniParcEntry> {
    private static final String UNIPARC_ID_FIELD = "upi";
    private static final String ACCESSION_STR = "accession";
    private static final String DB_ID_STR = "dbid";
    private static final String UP_ID_STR = "upid";
    private static final String COMMA_STR = ",";
    private final SearchFieldConfig searchFieldConfig;
    private final DefaultSearchQueryOptimiser defaultSearchQueryOptimiser;
    private final UniParcQueryRepository repository;
    private final UniParcQueryResultConverter entryConverter;

    @Autowired
    public UniParcQueryService(
            UniParcQueryRepository repository,
            UniParcFacetConfig facetConfig,
            UniParcSortClause solrSortClause,
            UniParcQueryResultConverter uniParcQueryResultConverter,
            StoreStreamer<UniParcEntry> storeStreamer,
            QueryBoosts uniParcQueryBoosts) {

        super(
                repository,
                uniParcQueryResultConverter,
                solrSortClause,
                facetConfig,
                storeStreamer,
                uniParcQueryBoosts);
        this.searchFieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPARC);
        this.defaultSearchQueryOptimiser =
                new DefaultSearchQueryOptimiser(getDefaultSearchOptimisedFieldItems());
        this.repository = repository;
        this.entryConverter = uniParcQueryResultConverter;
    }

    public UniParcEntry findByAccession(UniParcGetByAccessionRequest getByAccessionRequest) {

        UniParcEntry uniParcEntry = getEntity(ACCESSION_STR, getByAccessionRequest.getAccession());

        return filterUniParcStream(Stream.of(uniParcEntry), getByAccessionRequest)
                .findFirst()
                .orElse(null);
    }

    public QueryResult<UniParcEntry> findByDbId(UniParcGetByDbIdRequest getByDbIdRequest) {
        return findByFieldId(DB_ID_STR, getByDbIdRequest.getDbId(), getByDbIdRequest);
    }

    public QueryResult<UniParcEntry> findByUpId(UniParcGetByUpIdRequest getByUpIdRequest) {
        return findByFieldId(UP_ID_STR, getByUpIdRequest.getUpId(), getByUpIdRequest);
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
    protected DefaultSearchQueryOptimiser getDefaultSearchQueryOptimiser() {
        return defaultSearchQueryOptimiser;
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems() {
        return Collections.singletonList(getIdField());
    }

    private QueryResult<UniParcEntry> findByFieldId(
            String fieldName, String fieldValue, UniParcGetByIdRequest request) {

        // search uniparc ids from solr
        UniParcSearchRequest searchRequest = createSolrSearchByIdRequest(fieldName, fieldValue);
        SolrRequest solrRequest = createSearchSolrRequest(searchRequest);
        QueryResult<UniParcDocument> results =
                repository.searchPage(solrRequest, searchRequest.getCursor());

        // convert doc to entries
        Stream<UniParcEntry> converted =
                results.getContent().map(entryConverter).filter(Objects::nonNull);
        // filter the entries
        Stream<UniParcEntry> filtered = filterUniParcStream(converted, request);
        return QueryResult.of(filtered, results.getPage());
    }

    private Stream<UniParcEntry> filterUniParcStream(
            Stream<UniParcEntry> uniParcEntryStream, UniParcGetByIdRequest request) {
        // convert comma separated values to list
        List<String> databases = csvToList(request.getDbTypes());
        List<String> databaseIds = csvToList(request.getDbIds());
        List<String> toxonomyIds = csvToList(request.getTaxonIds());
        // converters
        UniParcDatabaseFilter dbFilter = new UniParcDatabaseFilter();
        UniParcDatabaseIdFilter dbIdFilter = new UniParcDatabaseIdFilter();
        UniParcTaxonomyFilter taxonFilter = new UniParcTaxonomyFilter();
        UniParcDatabaseStatusFilter statusFilter = new UniParcDatabaseStatusFilter();

        // filter the results
        return uniParcEntryStream
                .map(uniParcEntry -> dbFilter.apply(uniParcEntry, databases))
                .map(uniParcEntry -> dbIdFilter.apply(uniParcEntry, databaseIds))
                .map(uniParcEntry -> taxonFilter.apply(uniParcEntry, toxonomyIds))
                .map(uniParcEntry -> statusFilter.apply(uniParcEntry, request.getActive()));
    }

    private UniParcSearchRequest createSolrSearchByIdRequest(String fieldName, String fieldValue) {
        UniParcSearchRequest searchRequest = new UniParcSearchRequest();
        searchRequest.setQuery(fieldName + ":" + fieldValue);
        return searchRequest;
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
