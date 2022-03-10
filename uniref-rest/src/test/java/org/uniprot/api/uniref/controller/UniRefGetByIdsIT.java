package org.uniprot.api.uniref.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import java.util.Arrays;
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
import org.uniprot.api.rest.respository.facet.impl.UniRefFacetConfig;
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

/**
 * @author sahmad
 * @created 22/03/2021
 */
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniRefEntryLightController.class)
@ExtendWith(value = SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniRefGetByIdsIT extends AbstractGetByIdsControllerIT {
    private static final String GET_BY_IDS_PATH = "/uniref/ids";
    private final UniRefDocumentConverter documentConverter =
            new UniRefDocumentConverter(TaxonomyRepoMocker.getTaxonomyRepo());
    private static final String TEST_IDS =
            "UniRef50_P03901,UniRef90_P03901,UniRef100_P03901,UniRef50_P03902,"
                    + "UniRef90_P03902,UniRef100_P03902,UniRef50_P03903,UniRef90_P03903,UniRef100_P03903,UniRef50_P03904";
    private static final String[] TEST_IDS_ARRAY = {
        "UniRef50_P03901",
        "UniRef90_P03901",
        "UniRef100_P03901",
        "UniRef50_P03902",
        "UniRef90_P03902",
        "UniRef100_P03902",
        "UniRef50_P03903",
        "UniRef90_P03903",
        "UniRef100_P03903",
        "UniRef50_P03904"
    };
    private static final String[] TEST_IDS_ARRAY_SORTED = {
        "UniRef100_P03901",
        "UniRef100_P03902",
        "UniRef100_P03903",
        "UniRef50_P03901",
        "UniRef50_P03902",
        "UniRef50_P03903",
        "UniRef50_P03904",
        "UniRef90_P03901",
        "UniRef90_P03902",
        "UniRef90_P03903"
    };
    private static final String MISSING_ID1 = "UniRef50_P00000";
    private static final String MISSING_ID2 = "UniRef90_Q00000";

    @Autowired private UniRefFacetConfig facetConfig;

    @Autowired UniProtStoreClient<UniRefEntryLight> storeClient;
    @Autowired private MockMvc mockMvc;
    @Autowired private FacetTupleStreamTemplate facetTupleStreamTemplate;
    @Autowired private TupleStreamTemplate tupleStreamTemplate;

    @BeforeAll
    void saveEntriesInSolrAndStore() throws Exception {
        saveEntries();
    }

    private void saveEntries() throws Exception {
        for (int i = 1; i <= 4; i++) {
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

    @Test
    void getByIdsWithQueryFilterSuccess() throws Exception {
        String queryFilter = "uniprot_id:P12301";
        // when
        ResultActions response =
                mockMvc.perform(
                        get(GET_BY_IDS_PATH)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("ids", getCommaSeparatedIds())
                                .param("query", queryFilter)
                                .param("fields", "id,name,types")
                                .param("size", "10"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(3)))
                .andExpect(
                        jsonPath(
                                "$.results.*.id",
                                contains("UniRef100_P03901", "UniRef50_P03901", "UniRef90_P03901")))
                .andExpect(jsonPath("$.facets").doesNotExist());
    }

    @Test
    void getByIdsQueryBadRequest() throws Exception {
        String queryFilter = "invalid:P12301 AND id:INVALID";
        // when
        ResultActions response =
                mockMvc.perform(
                        get(GET_BY_IDS_PATH)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("ids", getCommaSeparatedIds())
                                .param("query", queryFilter)
                                .param("fields", "id,name,types")
                                .param("size", "10"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.size()", is(2)))
                .andExpect(
                        jsonPath(
                                "$.messages",
                                containsInAnyOrder(
                                        "The 'id' value has invalid format. It should be a valid UniRef Cluster id",
                                        "'invalid' is not a valid search field")))
                .andExpect(jsonPath("$.facets").doesNotExist());
    }

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return List.of(SolrCollection.uniref);
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
        return csvs + "," + MISSING_ID1 + "," + MISSING_ID2;
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
        return GET_BY_IDS_PATH;
    }

    @Override
    protected String getRequestParamName() {
        return "ids";
    }

    @Override
    protected String getCommaSeparatedReturnFields() {
        return "id,name,length,organism_id";
    }

    @Override
    protected List<ResultMatcher> getResultsResultMatchers() {
        ResultMatcher rm1 = jsonPath("$.results.*.id", contains(TEST_IDS_ARRAY));
        ResultMatcher rm2 = jsonPath("$.results.*.name").exists();
        ResultMatcher rm3 = jsonPath("$.results.*.updated").exists();
        ResultMatcher rm4 = jsonPath("$.results.*.entryType").exists();
        ResultMatcher rm5 = jsonPath("$.results.*.memberCount").exists();
        ResultMatcher rm6 = jsonPath("$.results.*.organismCount").exists();
        ResultMatcher rm7 = jsonPath("$.results.*.seedId").exists();
        ResultMatcher rm8 = jsonPath("$.results.*.commonTaxon").exists();
        ResultMatcher rm9 = jsonPath("$.results.*.representativeMember").exists();
        ResultMatcher rm10 = jsonPath("$.results[0].memberIdTypes").isArray();
        ResultMatcher rm11 = jsonPath("$.results[0].members").isArray();
        ResultMatcher rm12 = jsonPath("$.results[0].organisms").isArray();
        ResultMatcher rm13 = jsonPath("$.results[0].goTerms").isArray();
        return List.of(rm1, rm2, rm3, rm4, rm5, rm6, rm7, rm8, rm9, rm10, rm11, rm12, rm13);
    }

    @Override
    protected List<ResultMatcher> getFacetsResultMatchers() {
        ResultMatcher rm1 = jsonPath("$.facets", iterableWithSize(1));
        ResultMatcher rm2 = jsonPath("$.facets.*.label", contains("Clusters"));
        ResultMatcher rm3 = jsonPath("$.facets.*.name", contains("identity"));
        ResultMatcher rm4 = jsonPath("$.facets[0].values", iterableWithSize(3));
        ResultMatcher rm11 = jsonPath("$.facets[0].values[0].label", is("100%"));
        ResultMatcher rm12 = jsonPath("$.facets[0].values[0].value", is("1.0"));
        ResultMatcher rm13 = jsonPath("$.facets[0].values[0].count", is(3));
        ResultMatcher rm8 = jsonPath("$.facets[0].values[1].label", is("90%"));
        ResultMatcher rm9 = jsonPath("$.facets[0].values[1].value", is("0.9"));
        ResultMatcher rm10 = jsonPath("$.facets[0].values[1].count", is(3));
        ResultMatcher rm5 = jsonPath("$.facets[0].values[2].label", is("50%"));
        ResultMatcher rm6 = jsonPath("$.facets[0].values[2].value", is("0.5"));
        ResultMatcher rm7 = jsonPath("$.facets[0].values[2].count", is(4));

        return List.of(rm1, rm2, rm3, rm4, rm5, rm6, rm7, rm8, rm9, rm10, rm11, rm12, rm13);
    }

    @Override
    protected List<ResultMatcher> getIdsAsResultMatchers() {
        return Arrays.stream(TEST_IDS_ARRAY)
                .map(id -> content().string(containsString(id)))
                .collect(Collectors.toList());
    }

    @Override
    protected List<ResultMatcher> getFieldsResultMatchers() {
        ResultMatcher rm1 = jsonPath("$.results.*.id", contains(TEST_IDS_ARRAY));
        ResultMatcher rm2 = jsonPath("$.results.*.name").exists();
        ResultMatcher rm3 = jsonPath("$.results.*.updated").doesNotExist();
        ResultMatcher rm4 = jsonPath("$.results.*.entryType").doesNotExist();
        ResultMatcher rm5 = jsonPath("$.results.*.memberCount").doesNotExist();
        ResultMatcher rm6 = jsonPath("$.results.*.organismCount").doesNotExist();
        ResultMatcher rm7 = jsonPath("$.results.*.seedId").doesNotExist();
        ResultMatcher rm8 = jsonPath("$.results.*.commonTaxon").doesNotExist();
        ResultMatcher rm9 = jsonPath("$.results.*.representativeMember").exists();
        ResultMatcher rm10 = jsonPath("$.results[0].memberIdTypes").doesNotExist();
        ResultMatcher rm11 = jsonPath("$.results[0].members").isArray();
        ResultMatcher rm12 = jsonPath("$.results[0].organisms").isArray();
        ResultMatcher rm13 = jsonPath("$.results[0].goTerms").doesNotExist();
        return List.of(rm1, rm2, rm3, rm4, rm5, rm6, rm7, rm8, rm9, rm10, rm11, rm12, rm13);
    }

    @Override
    protected List<ResultMatcher> getFirstPageResultMatchers() {
        ResultMatcher rm1 =
                jsonPath(
                        "$.results.*.id",
                        contains(List.of(TEST_IDS_ARRAY).subList(0, 4).toArray()));
        ResultMatcher rm2 = jsonPath("$.facets", iterableWithSize(1));
        ResultMatcher rm3 = jsonPath("$.facets.*.label", contains("Clusters"));
        ResultMatcher rm4 = jsonPath("$.facets.*.name", contains("identity"));
        return List.of(rm1, rm2, rm3, rm4);
    }

    @Override
    protected List<ResultMatcher> getSecondPageResultMatchers() {
        ResultMatcher rm1 =
                jsonPath(
                        "$.results.*.id",
                        contains(List.of(TEST_IDS_ARRAY).subList(4, 8).toArray()));
        return List.of(rm1);
    }

    @Override
    protected List<ResultMatcher> getThirdPageResultMatchers() {
        ResultMatcher rm1 =
                jsonPath(
                        "$.results.*.id",
                        contains(List.of(TEST_IDS_ARRAY).subList(8, 10).toArray()));
        return List.of(rm1);
    }

    @Override
    protected String[] getErrorMessages() {
        return new String[] {
            "Invalid fields parameter value 'invalid1'",
            "UniRef id 'INVALID2' has invalid format. It should be a valid UniRef id.",
            "Invalid fields parameter value 'invalid'",
            "UniRef id 'INVALID' has invalid format. It should be a valid UniRef id.",
            "The 'download' parameter has invalid format. It should be a boolean true or false."
        };
    }

    @Override
    protected String[] getInvalidFacetErrorMessage() {
        return new String[] {
            "Invalid facet name 'invalid_facet1'. Expected value can be [identity]."
        };
    }

    @Override
    protected String getQueryFilter() {
        return "identity:0.5 OR identity:0.9 OR identity:1.0";
    }

    @Override
    protected ResultMatcher getSortedIdResultMatcher() {
        return jsonPath("$.results.*.id", contains(TEST_IDS_ARRAY_SORTED));
    }

    @Override
    protected String getUnmatchedQueryFilter() {
        return "identity:2";
    }

    @Override
    protected String[] getIdLengthErrorMessage() {
        return new String[] {"Only '1000' UniRef ids are allowed in each request."};
    }
}
