package org.uniprot.api.idmapping.common.service.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.InvalidRequestException;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.rdf.RdfStreamer;
import org.uniprot.api.common.repository.stream.store.StoreRequest;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.common.repository.stream.store.StreamerConfigProperties;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageService;
import org.uniprot.api.idmapping.common.model.IdMappingResult;
import org.uniprot.api.idmapping.common.repository.UniprotKBMappingRepository;
import org.uniprot.api.idmapping.common.request.uniprotkb.UniProtKBIdMappingSearchRequest;
import org.uniprot.api.idmapping.common.request.uniprotkb.UniProtKBIdMappingStreamRequest;
import org.uniprot.api.idmapping.common.response.model.IdMappingStringPair;
import org.uniprot.api.idmapping.common.response.model.UniProtKBEntryPair;
import org.uniprot.api.idmapping.common.service.BasicIdService;
import org.uniprot.api.idmapping.common.service.store.impl.UniProtKBBatchStoreEntryPairIterable;
import org.uniprot.api.rest.output.converter.OutputFieldsParser;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.respository.facet.impl.UniProtKBFacetConfig;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.config.returnfield.model.ReturnField;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.datastore.UniProtStoreClient;

import net.jodah.failsafe.RetryPolicy;

/**
 * @author sahmad
 * @created 16/02/2021
 */
@Service
public class UniProtKBIdService extends BasicIdService<UniProtKBEntry, UniProtKBEntryPair> {

    public static final String ACCESSION = "accession_id";
    public static final String IS_ISOFORM = "is_isoform";
    public static final String SUB_SEQUENCE_PATTERN = ".*\\[\\d{1,5}-\\d{1,5}]";

    private final UniProtStoreClient<UniProtKBEntry> storeClient;

    private final RetryPolicy<Object> storeFetchRetryPolicy;

    private final StreamerConfigProperties streamConfig;

    private final UniprotKBMappingRepository repository;

    private final TaxonomyLineageService lineageService;

    private final ReturnFieldConfig returnFieldConfig;

    public UniProtKBIdService(
            @Qualifier("uniProtKBEntryStoreStreamer") StoreStreamer<UniProtKBEntry> storeStreamer,
            @Qualifier("uniproKBfacetTupleStreamTemplate") FacetTupleStreamTemplate tupleStream,
            @Qualifier("uniProtKBStoreRetryPolicy") RetryPolicy<Object> storeFetchRetryPolicy,
            @Qualifier("uniProtKBStreamerConfigProperties") StreamerConfigProperties streamConfig,
            UniprotKBMappingRepository repository,
            UniProtKBFacetConfig facetConfig,
            RdfStreamer idMappingRdfStreamer,
            UniProtStoreClient<UniProtKBEntry> storeClient,
            SolrQueryConfig uniProtKBSolrQueryConf,
            TaxonomyLineageService lineageService) {
        super(
                storeStreamer,
                tupleStream,
                facetConfig,
                idMappingRdfStreamer,
                uniProtKBSolrQueryConf);
        this.streamConfig = streamConfig;
        this.storeClient = storeClient;
        this.storeFetchRetryPolicy = storeFetchRetryPolicy;
        this.repository = repository;
        this.lineageService = lineageService;
        this.returnFieldConfig =
                ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIPROTKB);
    }

    @Override
    public QueryResult<UniProtKBEntryPair> getMappedEntries(
            SearchRequest searchRequest, IdMappingResult mappingResult, String jobId) {

        UniProtKBIdMappingSearchRequest kbIdMappingSearchRequest =
                (UniProtKBIdMappingSearchRequest) searchRequest;

        validateSubSequenceRequest(
                mappingResult.getMappedIds(), kbIdMappingSearchRequest.isSubSequence());

        return super.getMappedEntries(
                searchRequest, mappingResult, kbIdMappingSearchRequest.isIncludeIsoform(), jobId);
    }

    @Override
    protected Stream<UniProtKBEntry> getEntries(List<String> toIds, String fields) {
        StoreRequest storeRequest =
                StoreRequest.builder()
                        .addLineage(isLineageAllowed(fields, returnFieldConfig))
                        .build();
        return this.storeStreamer.streamEntries(toIds, storeRequest);
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
    protected Stream<UniProtKBEntryPair> streamEntries(
            List<IdMappingStringPair> mappedIds, StreamRequest streamRequest) {
        UniProtKBBatchStoreEntryPairIterable batchIterable =
                new UniProtKBBatchStoreEntryPairIterable(
                        mappedIds,
                        streamConfig.getStoreBatchSize(),
                        storeClient,
                        storeFetchRetryPolicy,
                        lineageService,
                        repository,
                        isLineageAllowed(streamRequest.getFields(), returnFieldConfig));
        return StreamSupport.stream(batchIterable.spliterator(), false).flatMap(Collection::stream);
    }

    @Override
    protected List<IdMappingStringPair> streamFilterAndSortEntries(
            StreamRequest streamRequest, List<IdMappingStringPair> mappedIds, String jobId) {
        UniProtKBIdMappingStreamRequest kbIdMappingStreamRequest =
                (UniProtKBIdMappingStreamRequest) streamRequest;
        validateSubSequenceRequest(mappedIds, kbIdMappingStreamRequest.isSubSequence());
        return super.streamFilterAndSortEntries(
                streamRequest, mappedIds, kbIdMappingStreamRequest.isIncludeIsoform(), jobId);
    }

    public static boolean isLineageAllowed(String fields, ReturnFieldConfig returnFieldConfig) {
        List<ReturnField> fieldList = OutputFieldsParser.parse(fields, returnFieldConfig);
        return fieldList.stream()
                .map(ReturnField::getName)
                .anyMatch(name -> "lineage".equals(name) || "lineage_ids".equals(name));
    }

    void validateSubSequenceRequest(List<IdMappingStringPair> mappedIds, boolean isSubsequence) {
        if (isSubsequence) {
            String invalidSubSequenceIds =
                    mappedIds.stream()
                            .map(IdMappingStringPair::getFrom)
                            .filter(id -> !id.matches(SUB_SEQUENCE_PATTERN))
                            .collect(Collectors.joining(","));
            if (!invalidSubSequenceIds.isEmpty()) {
                throw new InvalidRequestException(
                        "Unable to compute fasta subsequence for IDs: "
                                + invalidSubSequenceIds
                                + ". Expected format is accession[begin-end], for example:Q00001[10-20]");
            }
        }
    }
}
