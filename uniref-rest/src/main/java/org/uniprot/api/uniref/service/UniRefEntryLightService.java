package org.uniprot.api.uniref.service;

import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE_VALUE;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.ProblemPair;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.document.TupleStreamDocumentIdStream;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.respository.facet.impl.UniRefFacetConfig;
import org.uniprot.api.rest.service.StoreStreamerSearchService;
import org.uniprot.api.rest.service.query.config.UniRefSolrQueryConfig;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.uniref.repository.UniRefQueryRepository;
import org.uniprot.api.uniref.request.UniRefSearchRequest;
import org.uniprot.api.uniref.request.UniRefStreamRequest;
import org.uniprot.core.uniprotkb.taxonomy.Organism;
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
@Import(UniRefSolrQueryConfig.class)
public class UniRefEntryLightService
        extends StoreStreamerSearchService<UniRefDocument, UniRefEntryLight> {
    private final SolrQueryConfig solrQueryConfig;
    private static final int ID_LIMIT = 10;
    public static final String UNIREF_ID = "id";
    private final UniProtQueryProcessorConfig uniRefQueryProcessorConfig;
    private final SearchFieldConfig searchFieldConfig;
    private final RdfStreamer rdfStreamer;

    @Autowired
    public UniRefEntryLightService(
            UniRefQueryRepository repository,
            UniRefFacetConfig facetConfig,
            UniRefSortClause uniRefSortClause,
            UniRefLightQueryResultConverter uniRefQueryResultConverter,
            StoreStreamer<UniRefEntryLight> storeStreamer,
            SolrQueryConfig uniRefSolrQueryConf,
            UniProtQueryProcessorConfig uniRefQueryProcessorConfig,
            SearchFieldConfig uniRefSearchFieldConfig,
            RdfStreamer unirefRdfStreamer,
            FacetTupleStreamTemplate facetTupleStreamTemplate,
            TupleStreamDocumentIdStream solrIdStreamer) {
        super(
                repository,
                uniRefQueryResultConverter,
                uniRefSortClause,
                facetConfig,
                storeStreamer,
                uniRefSolrQueryConf,
                facetTupleStreamTemplate,
                solrIdStreamer);
        this.uniRefQueryProcessorConfig = uniRefQueryProcessorConfig;
        this.searchFieldConfig = uniRefSearchFieldConfig;
        this.solrQueryConfig = uniRefSolrQueryConf;
        this.rdfStreamer = unirefRdfStreamer;
    }

    @Override
    public UniRefEntryLight findByUniqueId(String uniqueId, String fields) {
        UniRefEntryLight entryLight = findByUniqueId(uniqueId);

        // clean unirefLight entry
        UniRefEntryLightBuilder builder = UniRefEntryLightBuilder.from(entryLight);

        // seedId can be size 1 (for UniParc Members. Example: UPI0005EFF57F)
        // or size2 (for UniProtMembers, Example: "FGFR2_HUMAN,P21802").
        // In this case bellow we need the UniParcId or Accession to display
        // in the search result (always the last index)
        String[] splittedSeed = entryLight.getSeedId().split(",");
        builder.seedId(splittedSeed[splittedSeed.length - 1]);

        List<String> members = removeMemberTypeFromMemberId(entryLight.getMembers());
        builder.membersSet(members);

        return builder.build();
    }

    @Override
    public QueryResult<UniRefEntryLight> search(SearchRequest request) {
        UniRefSearchRequest unirefRequest = (UniRefSearchRequest) request;
        QueryResult<UniRefEntryLight> result = super.search(request);
        Set<ProblemPair> warnings =
                getWarnings(
                        request.getQuery(), uniRefQueryProcessorConfig.getLeadingWildcardFields());
        if (!LIST_MEDIA_TYPE_VALUE.equals(request.getFormat())) {
            QueryResult.QueryResultBuilder<UniRefEntryLight> builder =
                    QueryResult.<UniRefEntryLight>builder()
                            .page(result.getPage())
                            .facets(result.getFacets())
                            .suggestions(result.getSuggestions())
                            .warnings(warnings);
            if (!unirefRequest.isComplete()) {
                Stream<UniRefEntryLight> content =
                        result.getContent().map(this::removeOverLimitAndCleanMemberId);
                builder.content(content);
            } else {
                Stream<UniRefEntryLight> content = result.getContent().map(this::cleanMemberId);
                builder.content(content);
            }
            result = builder.build();
        }
        return result;
    }

    @Override
    public Stream<UniRefEntryLight> stream(StreamRequest request) {
        UniRefStreamRequest uniRefRequest = (UniRefStreamRequest) request;
        Stream<UniRefEntryLight> result = super.stream(request);
        if (!LIST_MEDIA_TYPE_VALUE.equals(request.getFormat())) {
            if (!uniRefRequest.isComplete()) {
                result = result.map(this::removeOverLimitAndCleanMemberId);
            } else {
                result = result.map(this::cleanMemberId);
            }
        }
        return result;
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIREF;
    }

    @Override
    protected String getSolrIdField() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIREF)
                .getSearchFieldItemByName(UNIREF_ID)
                .getFieldName();
    }

    public Stream<String> streamRdf(
            UniRefStreamRequest streamRequest, String dataType, String format) {
        SolrRequest solrRequest =
                createSolrRequestBuilder(streamRequest, solrSortClause, solrQueryConfig).build();
        List<String> entryIds = solrIdStreamer.fetchIds(solrRequest).collect(Collectors.toList());
        return rdfStreamer.stream(entryIds.stream(), dataType, format);
    }

    @Override
    protected SearchFieldItem getIdField() {
        return this.searchFieldConfig.getSearchFieldItemByName(UNIREF_ID);
    }

    @Override
    protected UniProtQueryProcessorConfig getQueryProcessorConfig() {
        return uniRefQueryProcessorConfig;
    }

    @Override
    protected RdfStreamer getRdfStreamer() {
        return this.rdfStreamer;
    }

    @Override
    protected UniRefEntryLight mapToThinEntry(String uniRefId) {
        UniRefEntryLightBuilder builder = new UniRefEntryLightBuilder().id(uniRefId);
        return builder.build();
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

        if (entry.getOrganisms().size() > ID_LIMIT) {
            LinkedHashSet<Organism> organisms =
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
