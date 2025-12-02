package org.uniprot.api.uniprotkb.controller;

import static org.hamcrest.Matchers.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.rest.controller.AbstractGetByIdsPostControllerIT;
import org.uniprot.api.rest.request.IdsSearchRequest;
import org.uniprot.api.rest.respository.facet.impl.UniProtKBFacetConfig;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.common.repository.UniProtKBDataStoreTestConfig;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.request.UniProtKBIdsPostRequest;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.UniProtKBEntryType;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.indexer.uniprot.mockers.UniProtEntryMocker;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniprot.UniProtDocument;
import org.uniprot.store.spark.indexer.uniprot.converter.UniProtEntryConverter;

/**
 * @author sahmad
 * @since 2021-07-127
 */
@ContextConfiguration(classes = {UniProtKBDataStoreTestConfig.class, UniProtKBREST.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniProtKBPublicationController.class)
@AutoConfigureWebClient
@ExtendWith(value = {SpringExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniProtKBGetByAccessionsPostIT extends AbstractGetByIdsPostControllerIT {

    private static final String GET_BY_ACCESSIONS_PATH = "/uniprotkb/accessions";

    private static final UniProtKBEntry TEMPLATE_ENTRY =
            UniProtEntryMocker.create(UniProtEntryMocker.Type.SP_CANONICAL);

    private final UniProtEntryConverter documentConverter =
            new UniProtEntryConverter(new HashMap<>(), new HashMap<>());

    private static final String TEST_IDS =
            "p00003,P00002,P00001,P00007,P00006,P00005,P00004,P00008,P00010,P00009";
    private static final String[] TEST_IDS_ARRAY = {
        "P00003", "P00002", "P00001", "P00007", "P00006", "P00005", "P00004", "P00008", "P00010",
        "P00009"
    };
    private static final String[] TEST_IDS_ARRAY_SORTED = {
        "P00001", "P00002", "P00003", "P00004", "P00005", "P00006", "P00007", "P00008", "P00009",
        "P00010"
    };

    @Autowired private UniProtStoreClient<UniProtKBEntry> uniProtKBStoreClient;

    @Autowired private FacetTupleStreamTemplate facetTupleStreamTemplate;
    @Autowired private TupleStreamTemplate tupleStreamTemplate;

    @Autowired private MockMvc mockMvc;

    @Autowired private UniProtKBFacetConfig facetConfig;

    @BeforeAll
    void saveEntriesInSolrAndStore() throws Exception {
        for (int i = 1; i <= 10; i++) {
            UniProtKBEntryBuilder entryBuilder = UniProtKBEntryBuilder.from(TEMPLATE_ENTRY);
            String acc = String.format("P%05d", i);
            entryBuilder.primaryAccession(acc);
            if (i % 2 == 0) {
                entryBuilder.entryType(UniProtKBEntryType.SWISSPROT);
            } else {
                entryBuilder.entryType(UniProtKBEntryType.TREMBL);
            }

            UniProtKBEntry uniProtKBEntry = entryBuilder.build();

            UniProtDocument convert = documentConverter.convert(uniProtKBEntry);

            cloudSolrClient.addBean(SolrCollection.uniprot.name(), convert);
            uniProtKBStoreClient.saveEntry(uniProtKBEntry);
        }
        cloudSolrClient.commit(SolrCollection.uniprot.name());
    }

    @Override
    protected IdsSearchRequest getIdsDownloadWithFieldsRequest() {
        UniProtKBIdsPostRequest idsSearchRequest = new UniProtKBIdsPostRequest();
        idsSearchRequest.setAccessions(getCommaSeparatedIds());
        idsSearchRequest.setFields(getCommaSeparatedReturnFields());
        return idsSearchRequest;
    }

    @Override
    protected IdsSearchRequest getInvalidDownloadRequest() {
        UniProtKBIdsPostRequest idsSearchRequest = new UniProtKBIdsPostRequest();
        idsSearchRequest.setAccessions(getCommaSeparatedIds() + ",INVALID , INVALID2");
        idsSearchRequest.setFields("invalid, invalid1");
        return idsSearchRequest;
    }

    @Override
    protected IdsSearchRequest getIdsDownloadRequest() {
        UniProtKBIdsPostRequest idsSearchRequest = new UniProtKBIdsPostRequest();
        idsSearchRequest.setAccessions(getCommaSeparatedIds());
        return idsSearchRequest;
    }

    @Override
    protected String getCommaSeparatedIds() {
        return TEST_IDS;
    }

    @Override
    protected List<ResultMatcher> getResultsResultMatchers() {
        ResultMatcher rm1 =
                MockMvcResultMatchers.jsonPath(
                        "$.results.*.primaryAccession", contains(TEST_IDS_ARRAY));
        ResultMatcher rm2 =
                MockMvcResultMatchers.jsonPath(
                        "$.results[0].entryType", equalTo("UniProtKB unreviewed (TrEMBL)"));
        ResultMatcher rm3 =
                MockMvcResultMatchers.jsonPath("$.results[0].uniProtkbId", equalTo("FGFR2_HUMAN"));
        return List.of(rm1, rm2, rm3);
    }

    @Override
    protected List<ResultMatcher> getIdsAsResultMatchers() {
        return Arrays.stream(TEST_IDS_ARRAY)
                .map(id -> MockMvcResultMatchers.content().string(containsString(id)))
                .collect(Collectors.toList());
    }

    @Override
    protected MockMvc getMockMvc() {
        return this.mockMvc;
    }

    @Override
    protected String getGetByIdsPath() {
        return GET_BY_ACCESSIONS_PATH;
    }

    @Override
    protected String getCommaSeparatedReturnFields() {
        return "gene_names,organism_name";
    }

    @Override
    protected List<ResultMatcher> getFieldsResultMatchers() {
        ResultMatcher rm1 =
                MockMvcResultMatchers.jsonPath(
                        "$.results.*.primaryAccession", contains(TEST_IDS_ARRAY));
        ResultMatcher rm2 = MockMvcResultMatchers.jsonPath("$.results.*.organism").exists();
        ResultMatcher rm3 = MockMvcResultMatchers.jsonPath("$.results.*.genes").exists();
        ResultMatcher rm4 = MockMvcResultMatchers.jsonPath("$.results.*.sequence").doesNotExist();
        ResultMatcher rm5 = MockMvcResultMatchers.jsonPath("$.results.*.comments").doesNotExist();
        ResultMatcher rm6 =
                MockMvcResultMatchers.jsonPath(
                        "$.results[0].organism.scientificName", equalTo("Homo sapiens"));
        ResultMatcher rm7 =
                MockMvcResultMatchers.jsonPath(
                        "$.results[0].organism.commonName", equalTo("Human"));
        ResultMatcher rm8 =
                MockMvcResultMatchers.jsonPath("$.results[0].organism.taxonId", equalTo(9606));
        ResultMatcher rm9 =
                MockMvcResultMatchers.jsonPath(
                        "$.results[0].organism.lineage",
                        equalTo(TEMPLATE_ENTRY.getOrganism().getLineages()));
        return List.of(rm1, rm2, rm3, rm4, rm5, rm6, rm7, rm8, rm9);
    }

    @Override
    protected String[] getErrorMessages() {
        return new String[] {
            "Only '10' accessions are allowed in each request.",
            "Invalid fields parameter value 'invalid'",
            "Invalid fields parameter value 'invalid1'",
            "Accession 'INVALID' has invalid format. It should be a valid UniProtKB accession with optional sequence range e.g. P12345[10-20].",
            "Accession 'INVALID2' has invalid format. It should be a valid UniProtKB accession with optional sequence range e.g. P12345[10-20]."
        };
    }

    @Override
    protected String[] getErrorMessage() {
        return new String[] {"'accessions' is a required parameter"};
    }

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return Collections.singletonList(SolrCollection.uniprot);
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
    protected String getJsonRequestBodyWithFacets() {
        String facets = String.join(",", facetConfig.getFacetNames());
        StringBuilder builder = new StringBuilder("{");
        builder.append("\"accessions\":\"" + getCommaSeparatedIds() + "\",");
        builder.append("\"facets\":\"" + facets + "\"");
        builder.append("}");
        return builder.toString();
    }

    @Override
    protected String getJsonRequestBodyWithFacetFilter() {
        String facetFilter = "reviewed:true OR reviewed:false";
        StringBuilder builder = new StringBuilder("{");
        builder.append("\"accessions\":\"" + getCommaSeparatedIds() + "\",");
        builder.append("\"facetFilter\":\"" + facetFilter + "\"");
        builder.append("}");
        return builder.toString();
    }

    @Override
    protected String getJsonRequestBodyWithDownloadParam() {
        String download = "false";
        StringBuilder builder = new StringBuilder("{");
        builder.append("\"accessions\":\"" + getCommaSeparatedIds() + "\",");
        builder.append("\"download\":\"" + download + "\"");
        builder.append("}");
        return builder.toString();
    }
}
