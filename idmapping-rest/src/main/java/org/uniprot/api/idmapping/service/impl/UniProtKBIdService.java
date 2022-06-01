package org.uniprot.api.idmapping.service.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import net.jodah.failsafe.RetryPolicy;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.common.SolrDocument;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamer;
import org.uniprot.api.common.repository.stream.store.StoreStreamer;
import org.uniprot.api.common.repository.stream.store.StreamerConfigProperties;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.model.UniProtKBEntryPair;
import org.uniprot.api.idmapping.service.BasicIdService;
import org.uniprot.api.idmapping.service.store.impl.UniProtKBBatchStoreEntryPairIterable;
import org.uniprot.api.rest.respository.facet.impl.UniProtKBFacetConfig;
import org.uniprot.core.uniprotkb.InactiveReasonType;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.impl.EntryInactiveReasonBuilder;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

/**
 * @author sahmad
 * @created 16/02/2021
 */
@Service
public class UniProtKBIdService extends BasicIdService<UniProtKBEntry, UniProtKBEntryPair> {

    private final UniProtStoreClient<UniProtKBEntry> storeClient;

    private final RetryPolicy<Object> storeFetchRetryPolicy;

    private final StreamerConfigProperties streamConfig;

    private final SolrClient solrClient;

    public UniProtKBIdService(
            @Qualifier("uniProtKBEntryStoreStreamer") StoreStreamer<UniProtKBEntry> storeStreamer,
            @Qualifier("uniproKBfacetTupleStreamTemplate") FacetTupleStreamTemplate tupleStream,
            @Qualifier("uniProtKBStoreRetryPolicy") RetryPolicy<Object> storeFetchRetryPolicy,
            @Qualifier("uniProtKBStreamerConfigProperties") StreamerConfigProperties streamConfig,
            @Qualifier("uniProtKBSolrClient") SolrClient solrClient,
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
        this.solrClient = solrClient;
    }

    @Override
    protected UniProtKBEntryPair convertToPair(
            IdMappingStringPair mId, Map<String, UniProtKBEntry> idEntryMap) {
        UniProtKBEntry toEntry = idEntryMap.computeIfAbsent(mId.getTo(), this::getDeletedEntry);

        return UniProtKBEntryPair.builder().from(mId.getFrom()).to(toEntry).build();
    }

    private UniProtKBEntry getDeletedEntry(String accession) {
        UniProtKBEntry result = null;
        try {
            SolrDocument solrDocument =
                    solrClient.getById(SolrCollection.uniprot.toString(), accession);
            if (solrDocument != null) {
                UniProtDocument document =
                        new DocumentObjectBinder().getBean(UniProtDocument.class, solrDocument);
                if (document.active) {
                    throw new ServiceException(
                            "Error fetching deleted entry for id-mapping. accession:" + accession);
                } else {
                    EntryInactiveReasonBuilder inactiveReasonBuilder =
                            new EntryInactiveReasonBuilder().type(InactiveReasonType.DELETED);
                    result =
                            new UniProtKBEntryBuilder(
                                            accession, document.id, inactiveReasonBuilder.build())
                                    .build();
                }
            }
        } catch (SolrServerException | IOException e) {
            throw new ServiceException(
                    "Server error querying for deleted entries in Solr. accession:" + accession, e);
        }
        return result;
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
}
