package org.uniprot.api.uniparc.service;

import java.util.*;
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
import org.uniprot.api.uniparc.request.UniParcSearchRequest;
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

    public QueryResult<UniParcEntry> findByAccession(
            String accession, String dbTypes, String dbIds, String taxonIds, Boolean isActive) {

        UniParcSearchRequest searchRequest = createSearchRequest(accession);
        SolrRequest solrRequest = createSearchSolrRequest(searchRequest);
        QueryResult<UniParcDocument> results =
                repository.searchPage(solrRequest, searchRequest.getCursor());

        List<String> databases = csvToList(dbTypes);
        List<String> databaseIds = csvToList(dbIds);
        List<String> toxonomyIds = csvToList(taxonIds);
        UniParcDatabaseFilter dbFilter = new UniParcDatabaseFilter();
        UniParcDatabaseIdFilter dbIdFilter = new UniParcDatabaseIdFilter();
        UniParcTaxonomyFilter taxonFilter = new UniParcTaxonomyFilter();
        UniParcDatabaseStatusFilter statusFilter = new UniParcDatabaseStatusFilter();

        Stream<UniParcEntry> converted =
                results.getContent()
                        .map(entryConverter)
                        .filter(Objects::nonNull)
                        .map(uniParcEntry -> dbFilter.apply(uniParcEntry, databases))
                        .map(uniParcEntry -> dbIdFilter.apply(uniParcEntry, databaseIds))
                        .map(uniParcEntry -> taxonFilter.apply(uniParcEntry, toxonomyIds))
                        .map(uniParcEntry -> statusFilter.apply(uniParcEntry, isActive));

        return QueryResult.of(converted, results.getPage(), null);
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

    private UniParcSearchRequest createSearchRequest(String accession) {
        UniParcSearchRequest searchRequest = new UniParcSearchRequest();
        searchRequest.setQuery("accession:" + accession);
        return searchRequest;
    }

    private List<String> csvToList(String csv) {
        List<String> list = new ArrayList<>();
        if (Utils.notNullNotEmpty(csv)) {
            list = Arrays.stream(csv.split(",")).map(String::trim).collect(Collectors.toList());
        }
        return list;
    }
}
