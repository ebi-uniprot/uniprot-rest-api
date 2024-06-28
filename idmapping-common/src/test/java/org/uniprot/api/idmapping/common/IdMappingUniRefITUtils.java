package org.uniprot.api.idmapping.common;

import static org.uniprot.store.indexer.uniref.mockers.UniRefEntryMocker.ACC_PREF;
import static org.uniprot.store.indexer.uniref.mockers.UniRefEntryMocker.ID_PREF_50;

import org.apache.solr.client.solrj.impl.CloudSolrClient;
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

/**
 * @author lgonzales
 * @since 09/03/2021
 */
public class IdMappingUniRefITUtils {

    private static final UniRefDocumentConverter documentConverter =
            new UniRefDocumentConverter(TaxonomyRepoMocker.getTaxonomyRepo());

    public static String getUniRefFieldValueForValidatedField(String searchField) {
        String value = "*";
        switch (searchField) {
            case "id":
                value = ID_PREF_50 + 11;
                break;
            case "upi":
                value = "UPI0000083A11";
                break;
            case "taxonomy_id":
                value = "9600";
                break;
            case "uniprot_id":
                value = ACC_PREF + 11;
                break;
            case "count":
                value = "[2 TO 2]";
                break;
            case "length":
                value = "[10 TO 500]";
                break;
            case "created":
                value = "[2000-01-01 TO *]";
                break;
        }
        return value;
    }

    public static void saveEntries(
            CloudSolrClient cloudSolrClient, UniProtStoreClient<UniRefEntryLight> storeClient)
            throws Exception {
        for (int i = 1; i <= AbstractJobOperation.DEFAULT_IDS_COUNT; i++) {
            saveEntry(i, UniRefType.UniRef50, cloudSolrClient, storeClient);
            saveEntry(i, UniRefType.UniRef90, cloudSolrClient, storeClient);
            saveEntry(i, UniRefType.UniRef100, cloudSolrClient, storeClient);
        }
        cloudSolrClient.commit(SolrCollection.uniref.name());
    }

    private static void saveEntry(
            int i,
            UniRefType type,
            CloudSolrClient cloudSolrClient,
            UniProtStoreClient<UniRefEntryLight> storeClient)
            throws Exception {
        UniRefEntry entry = UniRefEntryMocker.createEntry(i, i, type);
        UniRefEntryConverter converter = new UniRefEntryConverter();
        Entry xmlEntry = converter.toXml(entry);
        UniRefEntryLightConverter unirefLightConverter = new UniRefEntryLightConverter();
        UniRefEntryLight entryLight = unirefLightConverter.fromXml(xmlEntry);
        UniRefDocument doc = documentConverter.convert(xmlEntry);
        cloudSolrClient.addBean(SolrCollection.uniref.name(), doc);
        storeClient.saveEntry(entryLight);
    }
}
