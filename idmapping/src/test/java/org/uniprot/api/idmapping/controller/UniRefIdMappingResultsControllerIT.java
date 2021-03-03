package org.uniprot.api.idmapping.controller;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.provider.Arguments;
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
import org.uniprot.api.rest.respository.facet.impl.UniRefFacetConfig;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.core.uniref.UniRefType;
import org.uniprot.core.xml.jaxb.uniref.Entry;
import org.uniprot.core.xml.uniref.UniRefEntryConverter;
import org.uniprot.core.xml.uniref.UniRefEntryLightConverter;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.uniref.UniRefDocumentConverter;
import org.uniprot.store.indexer.uniref.mockers.UniRefEntryMocker;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniref.UniRefDocument;

/**
 * @author lgonzales
 * @since 26/02/2021
 */
@ActiveProfiles(profiles = "offline")
@ContextConfiguration(classes = {DataStoreTestConfig.class, IdMappingREST.class})
@WebMvcTest(UniRefIdMappingResultsController.class)
@AutoConfigureWebClient
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UniRefIdMappingResultsControllerIT extends AbstractIdMappingResultsControllerIT {

    private static final String UNIREF_ID_MAPPING_RESULT = "/idmapping/uniref/results/{jobId}";

    private final UniRefDocumentConverter documentConverter =
            new UniRefDocumentConverter(TaxonomyRepoMocker.getTaxonomyRepo());

    @Autowired private UniRefFacetConfig facetConfig;

    @Autowired private UniProtStoreClient<UniRefEntryLight> storeClient;

    @Autowired private MockMvc mockMvc;

    @Qualifier("uniRefFacetTupleStreamTemplate")
    @Autowired
    private FacetTupleStreamTemplate facetTupleStreamTemplate;

    @Qualifier("uniRefTupleStreamTemplate")
    @Autowired
    private TupleStreamTemplate tupleStreamTemplate;

    @Autowired private JobOperation uniRefIdMappingJobOp;

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
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIREF;
    }

    @Override
    protected FacetConfig getFacetConfig() {
        return facetConfig;
    }

    @Override
    protected JobOperation getJobOperation() {
        return uniRefIdMappingJobOp;
    }

    @Override
    protected Stream<Arguments> getAllReturnedFields() {
        return ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIREF)
                .getReturnFields().stream()
                .map(
                        returnField -> {
                            String lightPath =
                                    returnField.getPaths().get(returnField.getPaths().size() - 1);
                            return Arguments.of(
                                    returnField.getName(), Collections.singletonList(lightPath));
                        });
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
