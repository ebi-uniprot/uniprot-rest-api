package org.uniprot.api.idmapping.common.service.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.common.repository.stream.store.StreamerConfigProperties;
import org.uniprot.api.idmapping.common.request.uniref.UniRefIdMappingSearchRequest;
import org.uniprot.api.idmapping.common.request.uniref.UniRefIdMappingStreamRequest;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.response.model.UniRefEntryPair;
import org.uniprot.api.idmapping.common.service.BasicIdService;
import org.uniprot.api.idmapping.common.service.store.impl.UniRefBatchStoreEntryPairIterable;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.respository.facet.impl.UniRefFacetConfig;
import org.uniprot.api.uniref.common.service.light.UniRefEntryLightUtils;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.datastore.UniProtStoreClient;

import net.jodah.failsafe.RetryPolicy;

/**
 * @author sahmad
 * @created 16/02/2021
 */
@Service
public class UniRefIdService extends BasicIdService<UniRefEntryLight, UniRefEntryPair> {

    private final UniProtStoreClient<UniRefEntryLight> storeClient;

    private final RetryPolicy<Object> storeFetchRetryPolicy;

    private final StreamerConfigProperties streamConfig;

    public UniRefIdService(
            @Qualifier("uniRefEntryStoreStreamer") StoreStreamer<UniRefEntryLight> storeStreamer,
            @Qualifier("uniRefFacetTupleStreamTemplate") FacetTupleStreamTemplate tupleStream,
            @Qualifier("uniRefStreamerConfigProperties") StreamerConfigProperties streamConfig,
            @Qualifier("uniRefStoreRetryPolicy") RetryPolicy<Object> storeFetchRetryPolicy,
            UniRefFacetConfig facetConfig,
            RdfStreamer idMappingRdfStreamer,
            UniProtStoreClient<UniRefEntryLight> storeClient,
            SolrQueryConfig uniRefSolrQueryConf) {
        super(storeStreamer, tupleStream, facetConfig, idMappingRdfStreamer, uniRefSolrQueryConf);
        this.streamConfig = streamConfig;
        this.storeClient = storeClient;
        this.storeFetchRetryPolicy = storeFetchRetryPolicy;
    }

    @Override
    protected Stream<UniRefEntryPair> getPagedEntries(
            List<IdMappingStringPair> mappedIdPairs,
            CursorPage cursorPage,
            SearchRequest searchRequest) {
        Stream<UniRefEntryPair> entryPairStream =
                super.getPagedEntries(mappedIdPairs, cursorPage, searchRequest);
        UniRefIdMappingSearchRequest request = (UniRefIdMappingSearchRequest) searchRequest;

        if (request.isComplete()) {
            return entryPairStream.map(this::mapCompleteEntryPair);
        } else {
            return entryPairStream.map(this::mapLimitedEntryPair);
        }
    }

    private UniRefEntryPair mapCompleteEntryPair(UniRefEntryPair uniRefEntryPair) {
        UniRefEntryLight cleanEntry = UniRefEntryLightUtils.cleanMemberId(uniRefEntryPair.getTo());
        return UniRefEntryPair.builder().from(uniRefEntryPair.getFrom()).to(cleanEntry).build();
    }

    private UniRefEntryPair mapLimitedEntryPair(UniRefEntryPair uniRefEntryPair) {
        UniRefEntryLight cleanEntry =
                UniRefEntryLightUtils.removeOverLimitAndCleanMemberId(uniRefEntryPair.getTo());
        return UniRefEntryPair.builder().from(uniRefEntryPair.getFrom()).to(cleanEntry).build();
    }

    @Override
    protected UniRefEntryPair convertToPair(
            IdMappingStringPair mId, Map<String, UniRefEntryLight> idEntryMap) {
        return UniRefEntryPair.builder()
                .from(mId.getFrom())
                .to(idEntryMap.get(mId.getTo()))
                .build();
    }

    @Override
    protected String getEntryId(UniRefEntryLight entry) {
        return entry.getId().getValue();
    }

    @Override
    protected String getSolrIdField() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIREF)
                .getSearchFieldItemByName("id")
                .getFieldName();
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIREF;
    }

    @Override
    protected Stream<UniRefEntryPair> streamEntries(
            List<IdMappingStringPair> mappedIds, StreamRequest streamRequest) {
        UniRefIdMappingStreamRequest request = (UniRefIdMappingStreamRequest) streamRequest;
        UniRefBatchStoreEntryPairIterable batchIterable =
                new UniRefBatchStoreEntryPairIterable(
                        mappedIds,
                        streamConfig.getStoreBatchSize(),
                        storeClient,
                        storeFetchRetryPolicy,
                        request.isComplete());
        return StreamSupport.stream(batchIterable.spliterator(), false).flatMap(Collection::stream);
    }
}
