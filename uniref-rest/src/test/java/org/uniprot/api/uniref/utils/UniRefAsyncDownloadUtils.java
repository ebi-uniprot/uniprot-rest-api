package org.uniprot.api.uniref.utils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.uniprot.api.rest.controller.AbstractStreamControllerIT.SAMPLE_RDF;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.api.uniref.repository.UniRefQueryRepository;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.core.uniref.UniRefType;
import org.uniprot.core.xml.jaxb.uniref.Entry;
import org.uniprot.core.xml.uniref.UniRefEntryConverter;
import org.uniprot.core.xml.uniref.UniRefEntryLightConverter;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.uniref.UniRefDocumentConverter;
import org.uniprot.store.indexer.uniref.mockers.UniRefEntryMocker;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniref.UniRefDocument;

public class UniRefAsyncDownloadUtils {

    public static final UniRefDocumentConverter documentConverter =
            new UniRefDocumentConverter(TaxonomyRepoMocker.getTaxonomyRepo());

    public static void setUp(RestTemplate restTemplate) {
        when(restTemplate.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());
        when(restTemplate.getForObject(any(), any())).thenReturn(SAMPLE_RDF);
    }

    public static void saveEntriesInSolrAndStore(
            UniRefQueryRepository unirefQueryRepository,
            CloudSolrClient cloudSolrClient,
            SolrClient solrClient,
            UniProtStoreClient<UniRefEntryLight> storeClient,
            String idsFolder,
            String resultFolder)
            throws Exception {
        ReflectionTestUtils.setField(unirefQueryRepository, "solrClient", cloudSolrClient);
        prepareDownloadFolders(idsFolder, resultFolder);
        saveEntries(cloudSolrClient, storeClient);
        long queryHits = 100L;
        QueryResponse response = mock(QueryResponse.class);
        SolrDocumentList results = mock(SolrDocumentList.class);
        when(results.getNumFound()).thenReturn(queryHits);
        when(response.getResults()).thenReturn(results);
        when(solrClient.query(anyString(), any())).thenReturn(response);
    }

    protected static void saveEntries(
            CloudSolrClient cloudSolrClient, UniProtStoreClient<UniRefEntryLight> storeClient)
            throws Exception {
        for (int i = 1; i <= 4; i++) {
            saveEntry(cloudSolrClient, i, UniRefType.UniRef50, storeClient);
            saveEntry(cloudSolrClient, i, UniRefType.UniRef90, storeClient);
            saveEntry(cloudSolrClient, i, UniRefType.UniRef100, storeClient);
        }
        cloudSolrClient.commit(SolrCollection.uniref.name());
    }

    private static void saveEntry(
            CloudSolrClient cloudSolrClient,
            int i,
            UniRefType type,
            UniProtStoreClient<UniRefEntryLight> storeClient)
            throws Exception {
        UniRefEntry entry = UniRefEntryMocker.createEntry(i, type);
        UniRefEntryConverter converter = new UniRefEntryConverter();
        Entry xmlEntry = converter.toXml(entry);
        UniRefEntryLightConverter unirefLightConverter = new UniRefEntryLightConverter();
        UniRefEntryLight entryLight = unirefLightConverter.fromXml(xmlEntry);
        UniRefDocument doc = documentConverter.convert(xmlEntry);
        cloudSolrClient.addBean(SolrCollection.uniref.name(), doc);
        storeClient.saveEntry(entryLight);
    }

    public static void prepareDownloadFolders(String idsFolder, String resultFolder)
            throws IOException {
        Files.createDirectories(Path.of(idsFolder));
        Files.createDirectories(Path.of(resultFolder));
    }
}
