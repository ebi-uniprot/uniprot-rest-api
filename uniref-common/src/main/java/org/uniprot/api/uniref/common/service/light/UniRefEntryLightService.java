package org.uniprot.api.uniref.common.service.light;

import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE_VALUE;
import static org.uniprot.api.uniref.common.service.light.UniRefEntryLightUtils.*;

import java.util.List;
import java.util.Set;
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
import org.uniprot.api.rest.service.request.RequestConverter;
import org.uniprot.api.uniref.common.repository.search.UniRefQueryRepository;
import org.uniprot.api.uniref.common.response.converter.UniRefLightQueryResultConverter;
import org.uniprot.api.uniref.common.service.light.request.UniRefSearchRequest;
import org.uniprot.api.uniref.common.service.light.request.UniRefStreamRequest;
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
    public static final String UNIREF_ID = "id";
    private final UniProtQueryProcessorConfig uniRefQueryProcessorConfig;
    private final SearchFieldConfig searchFieldConfig;
    private final RdfStreamer rdfStreamer;

    @Autowired
    public UniRefEntryLightService(
            UniRefQueryRepository repository,
            UniRefFacetConfig facetConfig,
            UniRefLightQueryResultConverter uniRefQueryResultConverter,
            StoreStreamer<UniRefEntryLight> storeStreamer,
            SolrQueryConfig uniRefSolrQueryConf,
            UniProtQueryProcessorConfig uniRefQueryProcessorConfig,
            SearchFieldConfig uniRefSearchFieldConfig,
            RdfStreamer uniRefRdfStreamer,
            FacetTupleStreamTemplate uniRefFacetTupleStreamTemplate,
            TupleStreamDocumentIdStream uniRefDocumentIdStream,
            RequestConverter uniRefRequestConverter) {
        super(
                repository,
                uniRefQueryResultConverter,
                facetConfig,
                storeStreamer,
                uniRefSolrQueryConf,
                uniRefFacetTupleStreamTemplate,
                uniRefDocumentIdStream,
                uniRefRequestConverter);
        this.uniRefQueryProcessorConfig = uniRefQueryProcessorConfig;
        this.searchFieldConfig = uniRefSearchFieldConfig;
        this.rdfStreamer = uniRefRdfStreamer;
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
                        result.getContent()
                                .map(UniRefEntryLightUtils::removeOverLimitAndCleanMemberId);
                builder.content(content);
            } else {
                Stream<UniRefEntryLight> content =
                        result.getContent().map(UniRefEntryLightUtils::cleanMemberId);
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
                result = result.map(UniRefEntryLightUtils::removeOverLimitAndCleanMemberId);
            } else {
                result = result.map(UniRefEntryLightUtils::cleanMemberId);
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
        SolrRequest solrRequest = getRequestConverter().createStreamSolrRequest(streamRequest);
        List<String> entryIds = solrIdStreamer.fetchIds(solrRequest).toList();
        return rdfStreamer.stream(entryIds, dataType, format);
    }

    @Override
    protected SearchFieldItem getIdField() {
        return this.searchFieldConfig.getSearchFieldItemByName(UNIREF_ID);
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
}
