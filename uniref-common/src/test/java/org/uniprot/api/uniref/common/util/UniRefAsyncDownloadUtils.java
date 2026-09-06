package org.uniprot.api.uniref.common.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.api.rest.controller.AbstractStreamControllerIT;
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
            SolrClient cloudSolrClient,
            UniProtStoreClient<UniRefEntryLight> storeClient,
            int entryCount,
            String accessionPrefix)
            throws Exception {
        saveEntries(cloudSolrClient, storeClient, entryCount, accessionPrefix);
    }

    public static void saveEntriesInSolrAndStore(
            SolrClient cloudSolrClient,
            UniProtStoreClient<UniRefEntryLight> storeClient,
            int entryCount)
            throws Exception {
        saveEntriesInSolrAndStore(cloudSolrClient, storeClient, entryCount, null);
    }

    protected static void saveEntries(
            SolrClient cloudSolrClient,
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
            SolrClient cloudSolrClient,
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
