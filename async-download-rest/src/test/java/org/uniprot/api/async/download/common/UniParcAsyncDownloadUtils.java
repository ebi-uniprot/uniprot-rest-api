package org.uniprot.api.async.download.common;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.api.rest.controller.AbstractStreamControllerIT;
import org.uniprot.api.uniparc.common.repository.search.UniParcQueryRepository;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.uniparc.impl.UniParcCrossReferencePair;
import org.uniprot.core.xml.jaxb.uniparc.Entry;
import org.uniprot.core.xml.uniparc.UniParcEntryConverter;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.indexer.converters.UniParcDocumentConverter;
import org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniparc.UniParcDocument;

public class UniParcAsyncDownloadUtils {

    public static final UniParcDocumentConverter documentConverter =
            new UniParcDocumentConverter(TaxonomyRepoMocker.getTaxonomyRepo(), new HashMap<>());

    public static void setUp(RestTemplate restTemplate) {
        when(restTemplate.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());
        when(restTemplate.getForObject(any(), any()))
                .thenReturn(AbstractStreamControllerIT.SAMPLE_RDF);
    }

    public static void saveEntriesInSolrAndStore(
            UniParcQueryRepository uniparcQueryRepository,
            CloudSolrClient cloudSolrClient,
            SolrClient solrClient,
            UniProtStoreClient<UniParcEntryLight> storeClient,
            UniProtStoreClient<UniParcCrossReferencePair> xrefStoreClient)
            throws Exception {
        ReflectionTestUtils.setField(uniparcQueryRepository, "solrClient", cloudSolrClient);
        saveEntries(cloudSolrClient, storeClient, xrefStoreClient);
        long queryHits = 100L;
        QueryResponse response = mock(QueryResponse.class);
        SolrDocumentList results = mock(SolrDocumentList.class);
        when(results.getNumFound()).thenReturn(queryHits);
        when(response.getResults()).thenReturn(results);
        when(solrClient.query(anyString(), any())).thenReturn(response);
    }

    protected static void saveEntries(
            CloudSolrClient cloudSolrClient,
            UniProtStoreClient<UniParcEntryLight> storeClient,
            UniProtStoreClient<UniParcCrossReferencePair> xrefStoreClient)
            throws Exception {
        for (int i = 1; i <= 12; i++) {
            saveEntry(cloudSolrClient, i, "upi" + i, storeClient, xrefStoreClient);
        }
        cloudSolrClient.commit(SolrCollection.uniparc.name());
    }

    private static void saveEntry(
            CloudSolrClient cloudSolrClient,
            int i,
            String upi,
            UniProtStoreClient<UniParcEntryLight> storeClient,
            UniProtStoreClient<UniParcCrossReferencePair> xrefStoreClient)
            throws Exception {
        UniParcEntry entry = UniParcEntryMocker.createUniParcEntry(i, upi);
        UniParcEntryConverter converter = new UniParcEntryConverter();
        Entry xmlEntry = converter.toXml(entry);
        UniParcDocument doc = documentConverter.convert(xmlEntry);
        cloudSolrClient.addBean(SolrCollection.uniparc.name(), doc);
        UniParcEntryLight lightEntry = UniParcEntryMocker.convertToUniParcEntryLight(entry);
        storeClient.saveEntry(lightEntry);
        // save cross references in store
        String xrefBatchKey = lightEntry.getUniParcId() + "_0";
        xrefStoreClient.saveEntry(
                new UniParcCrossReferencePair(xrefBatchKey, entry.getUniParcCrossReferences()));
    }
}
