package org.uniprot.api.uniref.service;

import static java.util.Arrays.asList;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.store.StoreStreamer;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.service.StoreStreamerSearchService;
import org.uniprot.api.rest.service.query.QueryProcessor;
import org.uniprot.api.rest.service.query.UniProtQueryProcessor;
import org.uniprot.api.uniref.repository.UniRefFacetConfig;
import org.uniprot.api.uniref.repository.UniRefQueryRepository;
import org.uniprot.api.uniref.request.UniRefSearchRequest;
import org.uniprot.api.uniref.request.UniRefStreamRequest;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.core.uniref.impl.UniRefEntryLightBuilder;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.uniref.UniRefDocument;

/**
 * @author jluo
 * @date: 20 Aug 2019
 */
@Service
@Import(UniRefQueryBoostsConfig.class)
public class UniRefLightSearchService
        extends StoreStreamerSearchService<UniRefDocument, UniRefEntryLight> {

    private static final int ID_LIMIT = 10;
    private final SearchFieldConfig searchFieldConfig;
    private final QueryProcessor queryProcessor;

    @Autowired
    public UniRefLightSearchService(
            UniRefQueryRepository repository,
            UniRefFacetConfig facetConfig,
            UniRefSortClause uniRefSortClause,
            UniRefLightQueryResultConverter uniRefQueryResultConverter,
            StoreStreamer<UniRefEntryLight> storeStreamer,
            QueryBoosts uniRefQueryBoosts) {
        super(
                repository,
                uniRefQueryResultConverter,
                uniRefSortClause,
                facetConfig,
                storeStreamer,
                uniRefQueryBoosts);
        this.searchFieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIREF);
        this.queryProcessor = new UniProtQueryProcessor(getDefaultSearchOptimisedFieldItems());
    }

    @Override
    public UniRefEntryLight findByUniqueId(String uniqueId, String fields) {
        return findByUniqueId(uniqueId);
    }

    @Override
    public QueryResult<UniRefEntryLight> search(SearchRequest request) {
        UniRefSearchRequest unirefRequest = (UniRefSearchRequest) request;
        QueryResult<UniRefEntryLight> result = super.search(request);
        if (!unirefRequest.isComplete()) {
            Stream<UniRefEntryLight> content = result.getContent().map(this::removeOverLimitIds);
            return QueryResult.of(content, result.getPage(), result.getFacets());
        }
        return result;
    }

    @Override
    public Stream<UniRefEntryLight> stream(StreamRequest request) {
        UniRefStreamRequest unirefRequest = (UniRefStreamRequest) request;
        Stream<UniRefEntryLight> result = super.stream(request);
        if (!unirefRequest.isComplete()) {
            result = result.map(this::removeOverLimitIds);
        }
        return result;
    }

    @Override
    public UniRefEntryLight findByUniqueId(String uniqueId) {
        throw new UnsupportedOperationException(
                "UniRefLightSearchService does not support findByUniqueId, try to use UniRefEntryService");
    }

    @Override
    public UniRefEntryLight getEntity(String idField, String value) {
        throw new UnsupportedOperationException(
                "UniRefLightSearchService does not support getEntity, try to use UniRefEntryService");
    }

    @Override
    protected SearchFieldItem getIdField() {
        return this.searchFieldConfig.getSearchFieldItemByName("id");
    }

    @Override
    protected QueryProcessor getQueryProcessor() {
        return queryProcessor;
    }

    private List<SearchFieldItem> getDefaultSearchOptimisedFieldItems() {
        return asList(
                searchFieldConfig.getSearchFieldItemByName("id"),
                searchFieldConfig.getSearchFieldItemByName("upi"));
    }

    private UniRefEntryLight removeOverLimitIds(UniRefEntryLight entry) {
        UniRefEntryLightBuilder builder = UniRefEntryLightBuilder.from(entry);
        if (entry.getMembers().size() > ID_LIMIT) {
            builder.membersSet(entry.getMembers().subList(0, ID_LIMIT));
        }
        if (entry.getOrganismIds().size() > ID_LIMIT) {
            LinkedHashSet<Long> organismIds =
                    entry.getOrganismIds().stream()
                            .limit(ID_LIMIT)
                            .collect(Collectors.toCollection(LinkedHashSet::new));
            builder.organismIdsSet(organismIds);
        }
        if (entry.getOrganisms().size() > ID_LIMIT) {
            LinkedHashSet<String> organisms =
                    entry.getOrganisms().stream()
                            .limit(ID_LIMIT)
                            .collect(Collectors.toCollection(LinkedHashSet::new));
            builder.organismsSet(organisms);
        }
        return builder.build();
    }
}
