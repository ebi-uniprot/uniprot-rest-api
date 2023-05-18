package org.uniprot.api.uniparc.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.uniprot.api.common.repository.solrstream.FacetTupleStreamTemplate;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.rest.controller.AbstractGetByIdsControllerIT;
import org.uniprot.api.rest.respository.facet.impl.UniParcFacetConfig;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.xml.jaxb.uniparc.Entry;
import org.uniprot.core.xml.uniparc.UniParcEntryConverter;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.indexer.uniparc.UniParcDocumentConverter;
import org.uniprot.store.indexer.uniparc.mockers.UniParcEntryMocker;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniparc.UniParcDocument;

/**
 * @author sahmad
 * @created 19/03/2021
 */
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniParcController.class)
@ExtendWith(value = SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniParcGetByUpisIT extends AbstractGetByIdsControllerIT {
    private static final String UPI_PREF = "UPI00000000";
    private static final String GET_BY_UPIS_PATH = "/uniparc/upis";
    private final UniParcDocumentConverter documentConverter =
            new UniParcDocumentConverter(TaxonomyRepoMocker.getTaxonomyRepo(), new HashMap<>());
    private static final String TEST_IDS =
            "UPI0000000004,UPI0000000003,UPI0000000002,UPI0000000001,UPI0000000005,"
                    + "UPI0000000006,UPI0000000007,UPI0000000010,UPI0000000009,UPI0000000008";
    private static final String[] TEST_IDS_ARRAY = {
        "UPI0000000004",
        "UPI0000000003",
        "UPI0000000002",
        "UPI0000000001",
        "UPI0000000005",
        "UPI0000000006",
        "UPI0000000007",
        "UPI0000000010",
        "UPI0000000009",
        "UPI0000000008"
    };
    private static final String[] TEST_IDS_ARRAY_SORTED = {
        "UPI0000000001",
        "UPI0000000002",
        "UPI0000000003",
        "UPI0000000004",
        "UPI0000000005",
        "UPI0000000006",
        "UPI0000000007",
        "UPI0000000008",
        "UPI0000000009",
        "UPI0000000010"
    };
    private static final String MISSING_ID1 = "UPI0000000050";
    private static final String MISSING_ID2 = "UPI0000000051";

    @Autowired UniProtStoreClient<UniParcEntry> storeClient;
    @Autowired private MockMvc mockMvc;
    @Autowired private FacetTupleStreamTemplate facetTupleStreamTemplate;
    @Autowired private TupleStreamTemplate tupleStreamTemplate;
    @Autowired private UniParcFacetConfig facetConfig;

    @BeforeAll
    void saveEntriesInSolrAndStore() throws Exception {
        saveEntries();
    }

    @Test
    void getByUIdsWithQueryFilterSuccess() throws Exception {
        String queryFilter = "uniprotkb:P10001 OR uniprotkb:P10002";
        // when
        ResultActions response =
                mockMvc.perform(
                        get(GET_BY_UPIS_PATH)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("upis", getCommaSeparatedIds())
                                .param("query", queryFilter)
                                .param("fields", "upi,organism")
                                .param("size", "10"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(2)))
                .andExpect(
                        jsonPath(
                                "$.results.*.uniParcId",
                                contains("UPI0000000001", "UPI0000000002")))
                .andExpect(jsonPath("$.facets").doesNotExist());
    }

    @Test
    void getByUIdsQueryBadRequest() throws Exception {
        String queryFilter = "invalid:P12301 AND upi:INVALID";
        // when
        ResultActions response =
                mockMvc.perform(
                        get(GET_BY_UPIS_PATH)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("upis", getCommaSeparatedIds())
                                .param("query", queryFilter)
                                .param("fields", "upi,organism")
                                .param("size", "10"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(2)))
                .andExpect(
                        jsonPath(
                                "$.messages",
                                containsInAnyOrder(
                                        "The 'upi' value has invalid format. It should be a valid UniParc UPI",
                                        "'invalid' is not a valid search field")))
                .andExpect(jsonPath("$.facets").doesNotExist());
    }

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return List.of(SolrCollection.uniparc);
    }

    @Override
    protected TupleStreamTemplate getTupleStreamTemplate() {
        return this.tupleStreamTemplate;
    }

    @Override
    protected FacetTupleStreamTemplate getFacetTupleStreamTemplate() {
        return this.facetTupleStreamTemplate;
    }

    @Override
    protected String getIdSortField() {
        return "upi";
    }

    @Override
    protected String getCommaSeparatedIds() {
        return TEST_IDS;
    }

    @Override
    protected String getCommaSeparatedNIds(int n) {
        return String.join(",", Arrays.asList(TEST_IDS_ARRAY).subList(0, n));
    }

    @Override
    protected String getCommaSeparatedMixedIds() {
        // 6 present and 2 missing ids
        String csvs = String.join(",", Arrays.asList(TEST_IDS_ARRAY).subList(0, 6));
        return MISSING_ID1 + "," + csvs + "," + MISSING_ID2;
    }

    @Override
    protected String getCommaSeparatedMissingIds() {
        return MISSING_ID1 + "," + MISSING_ID2;
    }

    @Override
    protected String getCommaSeparatedFacets() {
        return String.join(",", facetConfig.getFacetNames());
    }

    @Override
    protected MockMvc getMockMvc() {
        return this.mockMvc;
    }

    @Override
    protected String getGetByIdsPath() {
        return GET_BY_UPIS_PATH;
    }

    @Override
    protected String getRequestParamName() {
        return "upis";
    }

    @Override
    protected String getCommaSeparatedReturnFields() {
        return "gene,organism_id,CDD";
    }

    @Override
    protected List<ResultMatcher> getResultsResultMatchers() {
        ResultMatcher rm1 = jsonPath("$.results.*.uniParcId", contains(TEST_IDS_ARRAY));
        ResultMatcher rm2 = jsonPath("$.results[0].uniParcCrossReferences", iterableWithSize(3));
        ResultMatcher rm3 = jsonPath("$.results.*.sequence").exists();
        ResultMatcher rm4 = jsonPath("$.results.*.sequence", notNullValue());
        ResultMatcher rm5 = jsonPath("$.results[0].sequenceFeatures", iterableWithSize(13));
        return List.of(rm1, rm2, rm3, rm4, rm5);
    }

    @Override
    protected List<ResultMatcher> getFacetsResultMatchers() {
        ResultMatcher rm1 = jsonPath("$.facets", iterableWithSize(2));
        ResultMatcher rm2 = jsonPath("$.facets.*.label", contains("Database", "Organisms"));
        ResultMatcher rm3 =
                jsonPath("$.facets.*.name", contains("database_facet", "organism_name"));
        ResultMatcher rm4 = jsonPath("$.facets[0].values", iterableWithSize(2));
        ResultMatcher rm5 = jsonPath("$.facets[0].values[0].label", is("UniProtKB"));
        ResultMatcher rm6 = jsonPath("$.facets[0].values[0].value", is("100"));
        ResultMatcher rm7 = jsonPath("$.facets[0].values[0].count", is(10));
        ResultMatcher rm8 = jsonPath("$.facets[1].values", iterableWithSize(2));
        ResultMatcher rm9 = jsonPath("$.facets[1].values.*.label").doesNotExist();
        ResultMatcher rm10 =
                jsonPath(
                        "$.facets[1].values.*.value",
                        containsInAnyOrder("Homo sapiens", "Torpedo californica"));
        ResultMatcher rm11 = jsonPath("$.facets[1].values.*.count", containsInAnyOrder(10, 10));
        return List.of(rm1, rm2, rm3, rm4, rm5, rm6, rm7, rm8, rm9, rm10, rm11);
    }

    @Override
    protected List<ResultMatcher> getIdsAsResultMatchers() {
        return Arrays.stream(TEST_IDS_ARRAY)
                .map(id -> content().string(containsString(id)))
                .collect(Collectors.toList());
    }

    @Override
    protected List<ResultMatcher> getFieldsResultMatchers() {
        ResultMatcher rm1 = jsonPath("$.results.*.uniParcId", contains(TEST_IDS_ARRAY));
        ResultMatcher rm2 = jsonPath("$.results.*.sequence").doesNotExist();
        ResultMatcher rm3 = jsonPath("$.results.*.uniParcCrossReferences.*.geneName").exists();
        ResultMatcher rm4 =
                jsonPath("$.results.*.uniParcCrossReferences.*.organism.taxonId").exists();
        ResultMatcher rm5 = jsonPath("$.results.*.sequenceFeatures.*.database").exists();
        ResultMatcher rm6 = jsonPath("$.results[0].sequenceFeatures[0].database", is("CDD"));
        return List.of(rm1, rm2, rm3, rm4, rm5, rm6);
    }

    @Override
    protected List<ResultMatcher> getFirstPageResultMatchers() {
        ResultMatcher rm1 =
                jsonPath(
                        "$.results.*.uniParcId",
                        contains(List.of(TEST_IDS_ARRAY).subList(0, 4).toArray()));
        ResultMatcher rm2 = jsonPath("$.facets", iterableWithSize(2));
        ResultMatcher rm3 =
                jsonPath("$.facets.*.label", containsInAnyOrder("Organisms", "Database"));
        ResultMatcher rm4 =
                jsonPath("$.facets.*.name", containsInAnyOrder("organism_name", "database_facet"));
        return List.of(rm1, rm2, rm3, rm4);
    }

    @Override
    protected List<ResultMatcher> getSecondPageResultMatchers() {
        ResultMatcher rm1 =
                jsonPath(
                        "$.results.*.uniParcId",
                        contains(List.of(TEST_IDS_ARRAY).subList(4, 8).toArray()));
        return List.of(rm1);
    }

    @Override
    protected List<ResultMatcher> getThirdPageResultMatchers() {
        ResultMatcher rm1 =
                jsonPath(
                        "$.results.*.uniParcId",
                        contains(List.of(TEST_IDS_ARRAY).subList(8, 10).toArray()));
        return List.of(rm1);
    }

    @Override
    protected String[] getErrorMessages() {
        return new String[] {
            "UPI 'INVALID2' has invalid format. It should be a valid UniParc id.",
            "The 'download' parameter has invalid format. It should be a boolean true or false.",
            "Invalid fields parameter value 'invalid'",
            "UPI 'INVALID' has invalid format. It should be a valid UniParc id.",
            "Invalid fields parameter value 'invalid1'"
        };
    }

    @Override
    protected String[] getInvalidFacetErrorMessage() {
        return new String[] {
            "Invalid facet name 'invalid_facet1'. Expected value can be [database_facet, organism_name]."
        };
    }

    @Override
    protected String getQueryFilter() {
        return "database_facet:100";
    }

    @Override
    protected ResultMatcher getSortedIdResultMatcher() {
        return jsonPath("$.results.*.uniParcId", contains(TEST_IDS_ARRAY_SORTED));
    }

    @Override
    protected ResultMatcher getReverseSortedIdResultMatcher() {
        return jsonPath(
                "$.results.*.uniParcId",
                contains(
                        Arrays.stream(TEST_IDS_ARRAY_SORTED)
                                .sorted(Comparator.reverseOrder())
                                .collect(Collectors.toList())
                                .toArray()));
    }

    @Override
    protected String getUnmatchedQueryFilter() {
        return "organism_name:missing";
    }

    @Override
    protected String[] getIdLengthErrorMessage() {
        return new String[] {"Only '1000' upis are allowed in each request."};
    }

    @Override
    protected UniProtDataType getUniProtDataType() {
        return UniProtDataType.UNIPARC;
    }

    @Override
    public String getContentDisposition() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy_MM_dd");
        return "uniparc_" + now.format(dateTimeFormatter);
    }

    private void saveEntries() throws Exception {
        for (int i = 1; i <= 10; i++) {
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
