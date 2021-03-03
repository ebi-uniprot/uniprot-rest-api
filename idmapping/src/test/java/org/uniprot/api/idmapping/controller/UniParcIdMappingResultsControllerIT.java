package org.uniprot.api.idmapping.controller;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.idmapping.IdMappingREST;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.rest.respository.facet.impl.UniParcFacetConfig;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.xml.jaxb.uniparc.Entry;
import org.uniprot.core.xml.uniparc.UniParcEntryConverter;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.indexer.uniparc.UniParcDocumentConverter;
import org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.uniref.mockers.UniRefEntryMocker;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniparc.UniParcDocument;

/**
 * @author lgonzales
 * @since 26/02/2021
 */
@ActiveProfiles(profiles = "offline")
@ContextConfiguration(classes = {DataStoreTestConfig.class, IdMappingREST.class})
@WebMvcTest(UniParcIdMappingResultsController.class)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UniParcIdMappingResultsControllerIT extends AbstractIdMappingResultsControllerIT {

    private static final String UPI_PREF = "UPI0000283A";
    private static final String UNIPARC_ID_MAPPING_RESULT = "/idmapping/uniparc/results/{jobId}";

    private final UniParcDocumentConverter documentConverter =
            new UniParcDocumentConverter(TaxonomyRepoMocker.getTaxonomyRepo(), new HashMap<>());

    @Autowired private UniParcFacetConfig facetConfig;

    @Autowired private UniProtStoreClient<UniParcEntry> storeClient;

    @Autowired private MockMvc mockMvc;

    @Qualifier("uniParcFacetTupleStreamTemplate")
    @Autowired
    private FacetTupleStreamTemplate facetTupleStreamTemplate;

    @Qualifier("uniParcTupleStreamTemplate")
    @Autowired
    private TupleStreamTemplate tupleStreamTemplate;

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return List.of(SolrCollection.uniparc);
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
        return UNIPARC_ID_MAPPING_RESULT;
    }

    @Override
    protected IdMappingJob createAndPutJobInCache() throws Exception {
        Map<String, String> ids = new LinkedHashMap<>();
        for (int i = 1; i <= 20; i++) {
            String fromId = String.format("Q%05d", i);
            String toId = String.format(UPI_PREF + "%02d", i);
            ids.put(fromId, toId);
        }
        return createAndPutJobInCache("ACC", "UPARC", ids);
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIPARC;
    }

    @Override
    protected FacetConfig getFacetConfig() {
        return facetConfig;
    }

    @Override
    protected String getFieldValueForValidatedField(String fieldName) {
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

    @BeforeAll
    void saveEntriesStore() throws Exception {
        saveEntries();
    }

    private void saveEntries() throws Exception {
        for (int i = 1; i <= 20; i++) {
            saveEntry(i);
        }
        cloudSolrClient.commit(SolrCollection.uniparc.name());
    }

    private void saveEntry(int i) throws Exception {
        UniParcEntry entry = UniParcEntryMocker.createEntry(i, UPI_PREF);
        UniParcEntryConverter converter = new UniParcEntryConverter();
        Entry xmlEntry = converter.toXml(entry);
        UniParcDocument doc = documentConverter.convert(xmlEntry);
        cloudSolrClient.addBean(SolrCollection.uniparc.name(), doc);
        storeClient.saveEntry(entry);
    }
}
