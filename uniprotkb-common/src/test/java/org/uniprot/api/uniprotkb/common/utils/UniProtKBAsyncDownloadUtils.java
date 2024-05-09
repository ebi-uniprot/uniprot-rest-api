package org.uniprot.api.uniprotkb.common.utils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.uniprot.api.rest.controller.AbstractStreamControllerIT.SAMPLE_RDF;
import static org.uniprot.store.indexer.uniprot.mockers.InactiveEntryMocker.DELETED;

import java.util.HashMap;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.api.common.repository.stream.store.uniprotkb.TaxonomyLineageRepository;
import org.uniprot.api.uniprotkb.common.repository.search.UniprotQueryRepository;
import org.uniprot.core.json.parser.taxonomy.TaxonomyJsonConfig;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.core.taxonomy.TaxonomyRank;
import org.uniprot.core.taxonomy.impl.TaxonomyEntryBuilder;
import org.uniprot.core.taxonomy.impl.TaxonomyLineageBuilder;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.UniProtKBEntryType;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.cv.chebi.ChebiRepo;
import org.uniprot.cv.ec.ECRepo;
import org.uniprot.cv.go.GORepo;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.uniprot.inactiveentry.InactiveUniProtEntry;
import org.uniprot.store.indexer.uniprot.mockers.PathwayRepoMocker;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.uniprot.mockers.UniProtEntryMocker;
import org.uniprot.store.indexer.uniprotkb.converter.UniProtEntryConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

public class UniProtKBAsyncDownloadUtils {

    public static int totalNonIsoformEntries;

    public static final UniProtKBEntry TEMPLATE_ENTRY =
            UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);
    public static final UniProtEntryConverter documentConverter =
            new UniProtEntryConverter(
                    TaxonomyRepoMocker.getTaxonomyRepo(),
                    Mockito.mock(GORepo.class),
                    PathwayRepoMocker.getPathwayRepo(),
                    mock(ChebiRepo.class),
                    mock(ECRepo.class),
                    new HashMap<>());

    public static void setUp(RestTemplate restTemplate) {
        when(restTemplate.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());
        when(restTemplate.getForObject(any(), any())).thenReturn(SAMPLE_RDF);
    }

    public static void saveEntriesInSolrAndStore(
            UniprotQueryRepository uniprotQueryRepository,
            CloudSolrClient cloudSolrClient,
            SolrClient solrClient,
            UniProtStoreClient<UniProtKBEntry> storeClient,
            TaxonomyLineageRepository taxRepository)
            throws Exception {
        ReflectionTestUtils.setField(uniprotQueryRepository, "solrClient", cloudSolrClient);
        saveEntries(cloudSolrClient, storeClient);
        // for the following tests, ensure the number of hits
        // for each query is less than the maximum number allowed
        // to be streamed (configured in {@link
        // org.uniprot.api.common.repository.store.StreamerConfigProperties})
        long queryHits = 100L;
        QueryResponse response = mock(QueryResponse.class);
        SolrDocumentList results = mock(SolrDocumentList.class);
        when(results.getNumFound()).thenReturn(queryHits);
        when(response.getResults()).thenReturn(results);
        when(solrClient.query(anyString(), any())).thenReturn(response);

        ReflectionTestUtils.setField(taxRepository, "solrClient", cloudSolrClient);
    }

    public static void saveEntries(
            CloudSolrClient cloudSolrClient, UniProtStoreClient<UniProtKBEntry> storeClient)
            throws Exception {
        int i = 1;
        for (; i <= 10; i++) {
            saveEntry(cloudSolrClient, i, "", storeClient);
        }
        saveEntry(cloudSolrClient, i++, "-2", storeClient);
        saveEntry(cloudSolrClient, i++, "-2", storeClient);
        saveEntry(cloudSolrClient, i++, "", false, storeClient);
        saveEntry(cloudSolrClient, i, "", false, storeClient);
        totalNonIsoformEntries = i - 2;
        cloudSolrClient.commit(SolrCollection.uniprot.name());
        saveTaxonomyEntry(cloudSolrClient, 9606L);
        cloudSolrClient.commit(SolrCollection.taxonomy.name());
    }

    public static void saveEntry(
            CloudSolrClient cloudSolrClient,
            int i,
            String isoFormString,
            UniProtStoreClient<UniProtKBEntry> storeClient)
            throws Exception {
        saveEntry(cloudSolrClient, i, isoFormString, true, storeClient);
    }

    public static void saveInactiveEntries(DataStoreManager storeManager) {
        for (int i = 0; i < 2; i++) {
            InactiveUniProtEntry inactiveEntry =
                    InactiveUniProtEntry.from(
                            "I8FBX" + i, "INACTIVE_DROME", DELETED, null, "SOURCE_DELETION");
            storeManager.saveEntriesInSolr(
                    DataStoreManager.StoreType.INACTIVE_UNIPROT, inactiveEntry);
        }
    }

    public static void saveEntry(
            CloudSolrClient cloudSolrClient,
            int i,
            String isoFormString,
            boolean reviewed,
            UniProtStoreClient<UniProtKBEntry> storeClient)
            throws Exception {
        UniProtKBEntryBuilder entryBuilder = UniProtKBEntryBuilder.from(TEMPLATE_ENTRY);
        String acc = String.format("P%05d", i) + isoFormString;
        entryBuilder.primaryAccession(acc);
        if (reviewed) {
            entryBuilder.entryType(UniProtKBEntryType.SWISSPROT);
        } else {
            entryBuilder.entryType(UniProtKBEntryType.TREMBL);
        }
        UniProtKBEntry uniProtKBEntry = entryBuilder.build();
        UniProtDocument convert = documentConverter.convert(uniProtKBEntry);

        cloudSolrClient.addBean(SolrCollection.uniprot.name(), convert);
        storeClient.saveEntry(uniProtKBEntry);
    }

    public static void saveTaxonomyEntry(CloudSolrClient cloudSolrClient, long taxId)
            throws Exception {
        TaxonomyEntryBuilder entryBuilder = new TaxonomyEntryBuilder();
        TaxonomyEntry taxonomyEntry =
                entryBuilder
                        .taxonId(taxId)
                        .rank(TaxonomyRank.SPECIES)
                        .lineagesAdd(new TaxonomyLineageBuilder().taxonId(taxId + 1).build())
                        .lineagesAdd(new TaxonomyLineageBuilder().taxonId(taxId + 2).build())
                        .build();
        byte[] taxonomyObj =
                TaxonomyJsonConfig.getInstance()
                        .getFullObjectMapper()
                        .writeValueAsBytes(taxonomyEntry);

        TaxonomyDocument.TaxonomyDocumentBuilder docBuilder =
                TaxonomyDocument.builder()
                        .taxId(taxId)
                        .id(String.valueOf(taxId))
                        .taxonomyObj(taxonomyObj);
        cloudSolrClient.addBean(SolrCollection.taxonomy.name(), docBuilder.build());
    }
}
