package org.uniprot.api.idmapping.controller;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.uniprot.api.common.repository.search.facet.FacetConfig;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.idmapping.IdMappingREST;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.rest.respository.facet.impl.UniParcFacetConfig;
import org.uniprot.core.uniparc.UniParcDatabase;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.impl.UniParcEntryBuilder;
import org.uniprot.core.xml.jaxb.uniparc.Entry;
import org.uniprot.core.xml.uniparc.UniParcEntryConverter;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.indexer.uniparc.UniParcDocumentConverter;
import org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniparc.UniParcDocument;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    static final String UPI_PREF = "UPI0000283A";
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

    @Autowired private JobOperation uniParcIdMappingJobOp;

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
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIPARC;
    }

    @Override
    protected FacetConfig getFacetConfig() {
        return facetConfig;
    }

    @Override
    protected JobOperation getJobOperation() {
        return uniParcIdMappingJobOp;
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

    @Test
    void testIdMappingWithSuccess() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache();
        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .param("query", "database:EnsemblMetazoa")
                                        .param("facets", "organism_name,database")
                                        .param("fields", "upi,accession")
                                        .param("sort", "length desc")
                                        .param("size", "10")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.facets.size()", is(2)))
                .andExpect(jsonPath("$.facets.*.name", contains("organism_name","database")))
                .andExpect(jsonPath("$.facets[0].values.size()", is(2)))
                .andExpect(jsonPath("$.facets[0].values.*.value", contains("Homo sapiens", "Torpedo californica")))
                .andExpect(jsonPath("$.facets[0].values.*.count", contains(6, 6)))
                .andExpect(jsonPath("$.results.size()", is(6)))
                .andExpect(jsonPath("$.results.*.from", contains("Q00018","Q00015","Q00012","Q00009","Q00006","Q00003")))
                .andExpect(jsonPath("$.results.*.to.uniParcId", contains("UPI0000283A18","UPI0000283A15","UPI0000283A12","UPI0000283A09","UPI0000283A06","UPI0000283A03")))
                .andExpect(jsonPath("$.results.*.to.uniParcCrossReferences.*.database").exists())
                .andExpect(jsonPath("$.results.*.to.uniParcCrossReferences.*.organism").doesNotExist());
    }

    private void saveEntries() throws Exception {
        for (int i = 1; i <= 20; i++) {
            saveEntry(i);
        }
        cloudSolrClient.commit(SolrCollection.uniparc.name());
    }

    private void saveEntry(int i) throws Exception {
        UniParcEntryBuilder builder = UniParcEntryBuilder.from(UniParcEntryMocker.createEntry(i, UPI_PREF));
        if(i % 3 == 0){
            builder.uniParcCrossReferencesAdd(UniParcEntryMocker.getXref(UniParcDatabase.EG_METAZOA));
        }
        UniParcEntry entry = builder.build();
        UniParcEntryConverter converter = new UniParcEntryConverter();
        Entry xmlEntry = converter.toXml(entry);
        UniParcDocument doc = documentConverter.convert(xmlEntry);
        cloudSolrClient.addBean(SolrCollection.uniparc.name(), doc);
        storeClient.saveEntry(entry);
    }
}
