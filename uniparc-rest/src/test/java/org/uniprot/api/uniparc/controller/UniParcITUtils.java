package org.uniprot.api.uniparc.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker.convertToUniParcEntryLight;

import java.util.HashMap;
import java.util.List;

import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.ResultActions;
import org.uniprot.api.uniparc.common.repository.search.UniParcQueryRepository;
import org.uniprot.api.uniparc.common.repository.store.crossref.UniParcCrossReferenceStoreClient;
import org.uniprot.api.uniparc.common.repository.store.light.UniParcLightStoreClient;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.UniParcEntryLight;
import org.uniprot.core.uniparc.impl.UniParcCrossReferencePair;
import org.uniprot.core.util.Utils;
import org.uniprot.core.xml.jaxb.uniparc.Entry;
import org.uniprot.core.xml.uniparc.UniParcEntryConverter;
import org.uniprot.cv.taxonomy.TaxonomicNode;
import org.uniprot.cv.taxonomy.TaxonomyRepo;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.datastore.voldemort.light.uniparc.VoldemortInMemoryUniParcEntryLightStore;
import org.uniprot.store.datastore.voldemort.light.uniparc.crossref.VoldemortInMemoryUniParcCrossReferenceStore;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.uniparc.mockers.UniParcCrossReferenceMocker;
import org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.util.TaxonomyRepoUtil;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniparc.UniParcDocument;
import org.uniprot.store.search.document.uniparc.UniParcDocumentConverter;

public class UniParcITUtils {
    private static final String UPI_PREF = "UPI0000283A";

    private static final TaxonomyRepo taxonomyRepo = TaxonomyRepoMocker.getTaxonomyRepo();

    static UniParcDocument.UniParcDocumentBuilder getUniParcDocument(UniParcEntry entry) {
        UniParcDocumentConverter converter = new UniParcDocumentConverter();
        UniParcDocument doc = converter.convert(entry);
        UniParcDocument.UniParcDocumentBuilder builder = doc.toBuilder();
        for (UniParcCrossReference xref : entry.getUniParcCrossReferences()) {
            if (Utils.notNull(xref.getOrganism())) {
                List<TaxonomicNode> nodes =
                        TaxonomyRepoUtil.getTaxonomyLineage(
                                taxonomyRepo, (int) xref.getOrganism().getTaxonId());
                builder.organismId(nodes.get(0).id());
                builder.organismName(nodes.get(0).scientificName());
                nodes.forEach(
                        node -> {
                            builder.taxLineageId(node.id());
                            List<String> names = TaxonomyRepoUtil.extractTaxonFromNode(node);
                            names.forEach(builder::organismTaxon);
                        });
            }
        }
        return builder;
    }

    static void initStoreManager(DataStoreManager storeManager, UniParcQueryRepository repository) {
        storeManager.addSolrClient(DataStoreManager.StoreType.UNIPARC, SolrCollection.uniparc);
        ReflectionTestUtils.setField(
                repository,
                "solrClient",
                storeManager.getSolrClient(DataStoreManager.StoreType.UNIPARC));

        storeManager.addDocConverter(
                DataStoreManager.StoreType.UNIPARC,
                new org.uniprot.store.indexer.converters.UniParcDocumentConverter(
                        TaxonomyRepoMocker.getTaxonomyRepo(), new HashMap<>()));
        UniParcLightStoreClient uniParcLightStoreClient =
                new UniParcLightStoreClient(
                        VoldemortInMemoryUniParcEntryLightStore.getInstance("uniparc-light"));
        VoldemortInMemoryUniParcCrossReferenceStore xrefVDClient =
                VoldemortInMemoryUniParcCrossReferenceStore.getInstance("uniparc-cross-reference");
        UniParcCrossReferenceStoreClient crossRefStoreClient =
                new UniParcCrossReferenceStoreClient(xrefVDClient);
        storeManager.addStore(DataStoreManager.StoreType.UNIPARC_LIGHT, uniParcLightStoreClient);
        storeManager.addStore(
                DataStoreManager.StoreType.UNIPARC_CROSS_REFERENCE, crossRefStoreClient);
    }

    static void saveEntry(DataStoreManager storeManager, int xrefGroupSize, int qualifier) {
        int xrefCount = 25;
        UniParcEntry entry = UniParcEntryMocker.createUniParcEntry(qualifier, UPI_PREF, xrefCount);
        UniParcEntryConverter converter = new UniParcEntryConverter();
        Entry xmlEntry = converter.toXml(entry);
        storeManager.saveEntriesInSolr(DataStoreManager.StoreType.UNIPARC, xmlEntry);
        // uniparc light and its cross references in voldemort
        UniParcEntryLight uniParcEntryLight =
                UniParcEntryMocker.createUniParcEntryLight(qualifier, UPI_PREF, xrefCount);
        storeManager.saveToStore(DataStoreManager.StoreType.UNIPARC_LIGHT, uniParcEntryLight);
        List<UniParcCrossReferencePair> crossReferencePairs =
                UniParcCrossReferenceMocker.createUniParcCrossReferencePairs(
                        uniParcEntryLight.getUniParcId(), qualifier, xrefCount, xrefGroupSize);
        storeManager.saveToStore(
                DataStoreManager.StoreType.UNIPARC_CROSS_REFERENCE, crossReferencePairs);
    }

    static String extractCursor(ResultActions response, int cursorParameterIndex) {
        String linkHeader = response.andReturn().getResponse().getHeader(HttpHeaders.LINK);
        assertThat(linkHeader, notNullValue());
        String cursor = linkHeader.split("\\?")[1].split("&")[cursorParameterIndex].split("=")[1];
        assertThat(cursor, notNullValue());
        return cursor;
    }

    static void saveStreamEntries(
            int xrefGroupSize,
            CloudSolrClient cloudSolrClient,
            UniProtStoreClient<UniParcEntryLight> storeClient,
            UniParcCrossReferenceStoreClient xRefStoreClient)
            throws Exception {
        for (int i = 1; i <= 10; i++) {
            saveStreamEntry(i, xrefGroupSize, cloudSolrClient, storeClient, xRefStoreClient);
        }
        cloudSolrClient.commit(SolrCollection.uniparc.name());
    }

    static void saveStreamEntry(
            int i,
            int xrefGroupSize,
            CloudSolrClient cloudSolrClient,
            UniProtStoreClient<UniParcEntryLight> storeClient,
            UniParcCrossReferenceStoreClient xRefStoreClient)
            throws Exception {
        UniParcEntry entry = UniParcEntryMocker.createUniParcEntry(i, UPI_PREF);
        UniParcDocument.UniParcDocumentBuilder builder = getUniParcDocument(entry);
        cloudSolrClient.addBean(SolrCollection.uniparc.name(), builder.build());

        UniParcEntryLight entryLight = convertToUniParcEntryLight(entry);
        storeClient.saveEntry(entryLight);

        List<UniParcCrossReferencePair> xrefPairs =
                UniParcCrossReferenceMocker.createCrossReferencePairsFromXRefs(
                        entryLight.getUniParcId(),
                        xrefGroupSize,
                        entry.getUniParcCrossReferences());
        for (UniParcCrossReferencePair xrefPair : xrefPairs) {
            xRefStoreClient.saveEntry(xrefPair);
        }
    }
}
