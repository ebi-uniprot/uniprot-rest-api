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
            Stream<UniRefEntryLight> content =
                    result.getContent().map(this::removeOverLimitAndCleanMemberId);

            result = QueryResult.of(content, result.getPage(), result.getFacets());
        } else {
            Stream<UniRefEntryLight> content = result.getContent().map(this::cleanMemberId);

            result = QueryResult.of(content, result.getPage(), result.getFacets());
        }
        return result;
    }

    @Override
    public Stream<UniRefEntryLight> stream(StreamRequest request) {
        UniRefStreamRequest unirefRequest = (UniRefStreamRequest) request;
        Stream<UniRefEntryLight> result = super.stream(request);
        if (!unirefRequest.isComplete()) {
            result = result.map(this::removeOverLimitAndCleanMemberId);
        } else {
            result = result.map(this::cleanMemberId);
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

    private UniRefEntryLight cleanMemberId(UniRefEntryLight entry) {
        UniRefEntryLightBuilder builder = UniRefEntryLightBuilder.from(entry);

        List<String> members = removeMemberTypeFromMemberId(entry.getMembers());
        builder.membersSet(members);

        return builder.build();
    }

    private UniRefEntryLight removeOverLimitAndCleanMemberId(UniRefEntryLight entry) {
        UniRefEntryLightBuilder builder = UniRefEntryLightBuilder.from(entry);

        List<String> members = entry.getMembers();
        if (entry.getMembers().size() > ID_LIMIT) {
            members = entry.getMembers().subList(0, ID_LIMIT);
        }

        members = removeMemberTypeFromMemberId(members);
        builder.membersSet(members);

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

    /**
     * This method remove MemberIdType from member list and return just memberId
     *
     * @param members List of members that are stored in Voldemort with format:
     *     "memberId,MemberIdType"
     * @return List of return clean member with the format "memberId"
     */
    private List<String> removeMemberTypeFromMemberId(List<String> members) {
        return members.stream()
                .map(memberId -> memberId.split(",")[0])
                .collect(Collectors.toList());
    }
}
