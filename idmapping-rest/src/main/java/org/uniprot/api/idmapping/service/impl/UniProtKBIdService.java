package org.uniprot.api.idmapping.service.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.jodah.failsafe.RetryPolicy;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.common.repository.stream.store.StreamerConfigProperties;
import org.uniprot.api.idmapping.controller.request.uniprotkb.UniProtKBIdMappingSearchRequest;
import org.uniprot.api.idmapping.controller.request.uniprotkb.UniProtKBIdMappingStreamRequest;
import org.uniprot.api.idmapping.model.IdMappingResult;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.model.UniProtKBEntryPair;
import org.uniprot.api.idmapping.repository.UniprotKBMappingRepository;
import org.uniprot.api.idmapping.service.BasicIdService;
import org.uniprot.api.idmapping.service.store.impl.UniProtKBBatchStoreEntryPairIterable;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.respository.facet.impl.UniProtKBFacetConfig;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.datastore.UniProtStoreClient;

/**
 * @author sahmad
 * @created 16/02/2021
 */
@Service
public class UniProtKBIdService extends BasicIdService<UniProtKBEntry, UniProtKBEntryPair> {

    public static final String ACCESSION = "accession_id";
    public static final String IS_ISOFORM = "is_isoform";

    private final UniProtStoreClient<UniProtKBEntry> storeClient;

    private final RetryPolicy<Object> storeFetchRetryPolicy;

    private final StreamerConfigProperties streamConfig;

    private final UniprotKBMappingRepository repository;

    public UniProtKBIdService(
            @Qualifier("uniProtKBEntryStoreStreamer") StoreStreamer<UniProtKBEntry> storeStreamer,
            @Qualifier("uniproKBfacetTupleStreamTemplate") FacetTupleStreamTemplate tupleStream,
            @Qualifier("uniProtKBStoreRetryPolicy") RetryPolicy<Object> storeFetchRetryPolicy,
            @Qualifier("uniProtKBStreamerConfigProperties") StreamerConfigProperties streamConfig,
            UniprotKBMappingRepository repository,
            UniProtKBFacetConfig facetConfig,
            RDFStreamer uniProtKBRDFStreamer,
            UniProtStoreClient<UniProtKBEntry> storeClient,
            SolrQueryConfig uniProtKBSolrQueryConf) {
        super(
                storeStreamer,
                tupleStream,
                facetConfig,
                uniProtKBRDFStreamer,
                uniProtKBSolrQueryConf);
        this.streamConfig = streamConfig;
        this.storeClient = storeClient;
        this.storeFetchRetryPolicy = storeFetchRetryPolicy;
        this.repository = repository;
    }

    @Override
    public QueryResult<UniProtKBEntryPair> getMappedEntries(
            SearchRequest searchRequest, IdMappingResult mappingResult) {

        UniProtKBIdMappingSearchRequest kbIdMappingSearchRequest =
                (UniProtKBIdMappingSearchRequest) searchRequest;

        return super.getMappedEntries(
                searchRequest, mappingResult, kbIdMappingSearchRequest.isIncludeIsoform());
    }

    @Override
    protected UniProtKBEntryPair convertToPair(
            IdMappingStringPair mId, Map<String, UniProtKBEntry> idEntryMap) {
        UniProtKBEntry toEntry =
                idEntryMap.computeIfAbsent(mId.getTo(), repository::getDeletedEntry);

        return UniProtKBEntryPair.builder().from(mId.getFrom()).to(toEntry).build();
    }

    @Override
    protected String getEntryId(UniProtKBEntry entry) {
        return entry.getPrimaryAccession().getValue();
    }

    @Override
    protected String getSolrIdField() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPROTKB)
                .getSearchFieldItemByName("accession_id")
                .getFieldName();
    }

    @Override
    protected String getTermsQueryField() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPROTKB)
                .getSearchFieldItemByName("accession")
                .getFieldName();
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIPROTKB;
    }

    @Override
    protected Stream<UniProtKBEntryPair> streamEntries(List<IdMappingStringPair> mappedIds) {
        UniProtKBBatchStoreEntryPairIterable batchIterable =
                new UniProtKBBatchStoreEntryPairIterable(
                        mappedIds,
                        streamConfig.getStoreBatchSize(),
                        storeClient,
                        storeFetchRetryPolicy);
        return StreamSupport.stream(batchIterable.spliterator(), false).flatMap(Collection::stream);
    }

    @Override
    protected List<IdMappingStringPair> streamFilterAndSortEntries(
            StreamRequest streamRequest, List<IdMappingStringPair> mappedIds) {
        UniProtKBIdMappingStreamRequest kbIdMappingStreamRequest =
                (UniProtKBIdMappingStreamRequest) streamRequest;
        return super.streamFilterAndSortEntries(
                streamRequest, mappedIds, kbIdMappingStreamRequest.isIncludeIsoform());
    }
}
