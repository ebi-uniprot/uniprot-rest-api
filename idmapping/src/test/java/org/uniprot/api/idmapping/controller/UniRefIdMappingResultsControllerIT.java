package org.uniprot.api.idmapping.controller;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.idmapping.IDMappingREST;
import org.uniprot.api.idmapping.model.IdMappingJob;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lgonzales
 * @since 26/02/2021
 */
@ActiveProfiles(profiles = "offline")
@ContextConfiguration(classes = {DataStoreTestConfig.class, IDMappingREST.class})
@WebMvcTest(UniRefIdMappingResultsController.class)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UniRefIdMappingResultsControllerIT extends AbstractIdMappingResultsControllerIT{

    private static final String UNIREF_ID_MAPPING_RESULT =
            "/uniref/idmapping/results/{jobId}";

    private final UniRefDocumentConverter documentConverter =
            new UniRefDocumentConverter(TaxonomyRepoMocker.getTaxonomyRepo());

    @Autowired
    private UniProtStoreClient<UniRefEntryLight> storeClient;

    @Autowired
    private MockMvc mockMvc;

    @Qualifier("uniRefFacetTupleStreamTemplate")
    @Autowired
    private FacetTupleStreamTemplate facetTupleStreamTemplate;

    @Qualifier("uniRefTupleStreamTemplate")
    @Autowired
    private TupleStreamTemplate tupleStreamTemplate;

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return List.of(SolrCollection.uniref);
    }

    @Override
    protected TupleStreamTemplate getTupleStreamTemplate() {
        return tupleStreamTemplate;
    }

    @Override
    protected FacetTupleStreamTemplate getFacetTupleStreamTemplate() {
        return facetTupleStreamTemplate;
    }

    @Override
    protected MockMvc getMockMvc() {
        return mockMvc;
    }

    @Override
    protected String getIdMappingResultPath() {
        return UNIREF_ID_MAPPING_RESULT;
    }

    @Override
    protected IdMappingJob createAndPutJobInCache() throws Exception {
        Map<String, String> ids = new HashMap<>();
        for(int i=1; i <= 20; i++){
            String fromId = String.format("Q%05d", i);
            String toId = String.format(UniRefEntryMocker.ID_PREF_50+"%02d", i);
            ids.put(fromId, toId);
        }
        return createAndPutJobInCache("ACC", "NF50", ids);
    }


    @BeforeAll
    void saveEntriesStore() throws Exception {
        saveEntries();
    }

    private void saveEntries() throws Exception {
        for (int i = 1; i <= 20; i++) {
            saveEntry(i, UniRefType.UniRef50);
            saveEntry(i, UniRefType.UniRef90);
            saveEntry(i, UniRefType.UniRef100);
        }
        cloudSolrClient.commit(SolrCollection.uniref.name());
    }

    private void saveEntry(int i, UniRefType type) throws Exception {
        UniRefEntry entry = UniRefEntryMocker.createEntry(i, type);
        UniRefEntryConverter converter = new UniRefEntryConverter();
        Entry xmlEntry = converter.toXml(entry);
        UniRefEntryLightConverter unirefLightConverter = new UniRefEntryLightConverter();
        UniRefEntryLight entryLight = unirefLightConverter.fromXml(xmlEntry);
        UniRefDocument doc = documentConverter.convert(xmlEntry);
        cloudSolrClient.addBean(SolrCollection.uniref.name(), doc);
        storeClient.saveEntry(entryLight);
    }
}
