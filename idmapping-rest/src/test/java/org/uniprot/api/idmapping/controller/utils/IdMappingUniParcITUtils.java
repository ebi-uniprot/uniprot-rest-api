package org.uniprot.api.idmapping.controller.utils;

import java.util.HashMap;

import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.uniprot.core.uniparc.UniParcDatabase;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.impl.UniParcEntryBuilder;
import org.uniprot.core.xml.jaxb.uniparc.Entry;
import org.uniprot.core.xml.uniparc.UniParcEntryConverter;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.indexer.uniparc.UniParcDocumentConverter;
import org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniparc.UniParcDocument;

/**
 * @author lgonzales
 * @since 09/03/2021
 */
public class IdMappingUniParcITUtils {

    private static final UniParcDocumentConverter documentConverter =
            new UniParcDocumentConverter(TaxonomyRepoMocker.getTaxonomyRepo(), new HashMap<>());

    public static final String UPI_PREF = "UPI0000283A";

    public static String getUniParcFieldValueForValidatedField(String fieldName) {
        String value = "";
        switch (fieldName) {
            case "upid":
                value = "UP000005640";
                break;
            case "upi":
                value = UPI_PREF + 11;
                break;
            case "length":
                value = "[* TO *]";
                break;
            case "taxonomy_id":
                value = "9606";
                break;
            case "uniprotkb":
            case "isoform":
                value = "P10011";
                break;
        }
        return value;
    }

    public static void saveEntries(
            CloudSolrClient cloudSolrClient, UniProtStoreClient<UniParcEntry> storeClient)
            throws Exception {
        for (int i = 1; i <= AbstractJobOperation.DEFAULT_IDS_COUNT; i++) {
            saveEntry(i, cloudSolrClient, storeClient);
        }
        cloudSolrClient.commit(SolrCollection.uniparc.name());
    }

    private static void saveEntry(
            int i, CloudSolrClient cloudSolrClient, UniProtStoreClient<UniParcEntry> storeClient)
            throws Exception {
        UniParcEntryBuilder builder =
                UniParcEntryBuilder.from(UniParcEntryMocker.createEntry(i, UPI_PREF));
        if (i % 3 == 0) {
            builder.uniParcCrossReferencesAdd(
                    UniParcEntryMocker.getXref(UniParcDatabase.EG_METAZOA));
        }
        UniParcEntry entry = builder.build();
        UniParcEntryConverter converter = new UniParcEntryConverter();
        Entry xmlEntry = converter.toXml(entry);
        UniParcDocument doc = documentConverter.convert(xmlEntry);
        cloudSolrClient.addBean(SolrCollection.uniparc.name(), doc);
        storeClient.saveEntry(entry);
    }
}
