package org.uniprot.api.uniref.common.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.api.rest.controller.AbstractStreamControllerIT;
import org.uniprot.api.uniref.common.repository.search.UniRefQueryRepository;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.core.uniref.UniRefType;
import org.uniprot.core.xml.jaxb.uniref.Entry;
import org.uniprot.core.xml.uniref.UniRefEntryConverter;
import org.uniprot.core.xml.uniref.UniRefEntryLightConverter;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.indexer.converters.UniRefDocumentConverter;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.uniref.mockers.UniRefEntryMocker;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniref.UniRefDocument;

public class UniRefAsyncDownloadUtils {

    public static final UniRefDocumentConverter documentConverter =
            new UniRefDocumentConverter(TaxonomyRepoMocker.getTaxonomyRepo());

    public static void setUp(RestTemplate restTemplate) {
        when(restTemplate.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());
        when(restTemplate.getForObject(any(), any()))
                .thenReturn(AbstractStreamControllerIT.SAMPLE_RDF);
    }

    public static void saveEntriesInSolrAndStore(
            UniRefQueryRepository unirefQueryRepository,
            CloudSolrClient cloudSolrClient,
            SolrClient solrClient,
            UniProtStoreClient<UniRefEntryLight> storeClient,
            int entryCount,
            String accessionPrefix)
            throws Exception {
        ReflectionTestUtils.setField(unirefQueryRepository, "solrClient", cloudSolrClient);
        saveEntries(cloudSolrClient, storeClient, entryCount, accessionPrefix);
        long queryHits = 100L;
        QueryResponse response = mock(QueryResponse.class);
        SolrDocumentList results = mock(SolrDocumentList.class);
        when(results.getNumFound()).thenReturn(queryHits);
        when(response.getResults()).thenReturn(results);
        when(solrClient.query(anyString(), any())).thenReturn(response);
    }

    public static void saveEntriesInSolrAndStore(
            UniRefQueryRepository unirefQueryRepository,
            CloudSolrClient cloudSolrClient,
            SolrClient solrClient,
            UniProtStoreClient<UniRefEntryLight> storeClient,
            int entryCount)
            throws Exception {
        saveEntriesInSolrAndStore(
                unirefQueryRepository, cloudSolrClient, solrClient, storeClient, entryCount, null);
    }

    protected static void saveEntries(
            CloudSolrClient cloudSolrClient,
            UniProtStoreClient<UniRefEntryLight> storeClient,
            int entryCount,
            String accessionPrefix)
            throws Exception {
        for (int i = 1; i <= entryCount; i++) {
            saveEntry(cloudSolrClient, i, UniRefType.UniRef50, storeClient, accessionPrefix);
            saveEntry(cloudSolrClient, i, UniRefType.UniRef90, storeClient, accessionPrefix);
            saveEntry(cloudSolrClient, i, UniRefType.UniRef100, storeClient, accessionPrefix);
        }
        cloudSolrClient.commit(SolrCollection.uniref.name());
    }

    private static void saveEntry(
            CloudSolrClient cloudSolrClient,
            int i,
            UniRefType type,
            UniProtStoreClient<UniRefEntryLight> storeClient,
            String accessionPrefix)
            throws Exception {
        UniRefEntry entry;
        if (accessionPrefix != null) {
            entry = UniRefEntryMocker.createEntry(i, type, accessionPrefix);
        } else {
            entry = UniRefEntryMocker.createEntry(i, type);
        }

        UniRefEntryConverter converter = new UniRefEntryConverter();
        Entry xmlEntry = converter.toXml(entry);
        UniRefEntryLightConverter unirefLightConverter = new UniRefEntryLightConverter();
        UniRefEntryLight entryLight = unirefLightConverter.fromXml(xmlEntry);
        UniRefDocument doc = documentConverter.convert(xmlEntry);
        cloudSolrClient.addBean(SolrCollection.uniref.name(), doc);
        storeClient.saveEntry(entryLight);
    }
}
