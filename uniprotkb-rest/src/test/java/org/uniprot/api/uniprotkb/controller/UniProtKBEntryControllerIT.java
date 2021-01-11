package org.uniprot.api.uniprotkb.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.stream.IntStream;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.UniprotKBObjectsForTests;
import org.uniprot.api.uniprotkb.repository.DataStoreTestConfig;
import org.uniprot.api.uniprotkb.repository.search.impl.LiteratureRepository;
import org.uniprot.api.uniprotkb.repository.store.UniProtKBStoreClient;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.literature.LiteratureDocument;

/**
 * @author lgonzales
 * @since 2019-07-10
 */
@Slf4j
@ContextConfiguration(classes = {DataStoreTestConfig.class, UniProtKBREST.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniProtKBPublicationController.class)
@AutoConfigureWebClient
@ExtendWith(value = {SpringExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniProtKBEntryControllerIT {

    private static final String MAPPED_PROTEIN_PATH = "/uniprotkb/accession/";

    @Autowired private LiteratureRepository repository;

    @Autowired private UniProtKBStoreClient storeClient;

    @Autowired private MockMvc mockMvc;

    @RegisterExtension static DataStoreManager storeManager = new DataStoreManager();

    @BeforeAll
    void initSolrAndInjectItInTheRepository() {
        storeManager.addSolrClient(
                DataStoreManager.StoreType.LITERATURE, SolrCollection.literature);
        storeManager.addStore(DataStoreManager.StoreType.UNIPROT, storeClient);
        ReflectionTestUtils.setField(
                repository,
                "solrClient",
                storeManager.getSolrClient(DataStoreManager.StoreType.LITERATURE));
    }

    @BeforeEach
    void cleanData() {
        storeManager.cleanSolr(DataStoreManager.StoreType.LITERATURE);
        storeManager.cleanStore(DataStoreManager.StoreType.UNIPROT);
    }

    @Test
    void getPublicationsReturnSuccess() throws Exception {
        // given
        saveEntry(10, "P12309", "P12310");
        saveEntry(11, "P12310", "P12311");
        saveEntry(12, "P12311", "P12312");
        saveEntry(13, "P12312", "P12313");
        saveEntry(14, "P12313", "P12314");

        saveUniprotEntryInStore("P12312", "11");

        // when
        ResultActions response =
                mockMvc.perform(
                        get(MAPPED_PROTEIN_PATH + "P12312/publications")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(4)))
                .andExpect(
                        jsonPath(
                                "$.results[0].reference.citation.citationCrossReferences[0].id",
                                is("11")))
                .andExpect(
                        jsonPath("$.results[1].reference.citation.citationType", is("submission")))
                .andExpect(
                        jsonPath(
                                "$.results[2].reference.citation.citationCrossReferences[0].id",
                                is("12")))
                .andExpect(
                        jsonPath(
                                "$.results[3].reference.citation.citationCrossReferences[0].id",
                                is("13")))
                .andExpect(
                        jsonPath(
                                "$.results.*.reference.citation.title",
                                contains("title 11", "Submission tittle", "title 12", "title 13")))
                .andExpect(
                        jsonPath(
                                "$.results.*.categories",
                                contains(
                                        contains("Pathol", "Interaction"),
                                        contains("Interaction"),
                                        contains("Function"),
                                        contains("Function"))))
                .andExpect(
                        jsonPath(
                                "$.results.*.publicationSource",
                                contains(
                                        "UniProtKB reviewed (Swiss-Prot)",
                                        "UniProtKB reviewed (Swiss-Prot)",
                                        "Computationally mapped",
                                        "Computationally mapped")))
                .andExpect(
                        jsonPath(
                                "$.results.*.literatureMappedReference.uniprotAccession",
                                contains("P12312", "P12312")));
    }

    @Test
    void getPublicationsWithFacetReturnSuccess() throws Exception {
        // given
        saveEntry(10, "P12309", "P12310");
        saveEntry(11, "P12310", "P12311");
        saveEntry(12, "P12311", "P12312");
        saveEntry(13, "P12312", "P12313");
        saveEntry(14, "P12313", "P12314");

        saveUniprotEntryInStore("P12312", "10", "11");

        // when
        ResultActions response =
                mockMvc.perform(
                        get(MAPPED_PROTEIN_PATH + "P12312/publications")
                                .param("facets", "source,category,study_type")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(5)))
                .andExpect(
                        jsonPath("$.facets.*.label", contains("Source", "Category", "Study type")))
                .andExpect(
                        jsonPath("$.facets.*.name", contains("source", "category", "study_type")))
                .andExpect(
                        jsonPath(
                                "$.facets[0].values.*.label",
                                contains(
                                        "UniProtKB reviewed (Swiss-Prot)",
                                        "Computationally mapped")))
                .andExpect(
                        jsonPath(
                                "$.facets[0].values.*.value",
                                contains("uniprotkb_reviewed_swissprot", "computationally_mapped")))
                .andExpect(jsonPath("$.facets[0].values.*.count", contains(3, 2)))
                .andExpect(
                        jsonPath(
                                "$.facets[1].values.*.label",
                                contains("Interaction", "Function", "Pathol")))
                .andExpect(
                        jsonPath(
                                "$.facets[1].values.*.value",
                                contains("interaction", "function", "pathol")))
                .andExpect(jsonPath("$.facets[1].values.*.count", contains(3, 2, 2)))
                .andExpect(
                        jsonPath(
                                "$.facets[2].values.*.label",
                                contains("Large scale", "Small scale")))
                .andExpect(
                        jsonPath(
                                "$.facets[2].values.*.value",
                                contains("large_scale", "small_scale")))
                .andExpect(jsonPath("$.facets[2].values.*.count", contains(4, 1)));
    }

    @Test
    void getPublicationsWithFacetFiltersSuccess() throws Exception {
        // given
        saveEntry(10, "P12309", "P12310", "P12311");
        saveEntry(11, "P12310", "P12311", "P12312");
        saveEntry(12, "P12311", "P12312", "P12313");
        saveEntry(13, "P12312", "P12313", "P12314");
        saveEntry(14, "P12313", "P12314", "P12315");

        saveUniprotEntryInStore("P12312", "10", "11");

        // when
        ResultActions response =
                mockMvc.perform(
                        get(MAPPED_PROTEIN_PATH + "P12312/publications")
                                .param("facets", "source,category,study_type")
                                .param("query", "category:Interaction")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(3)))
                .andExpect(
                        jsonPath("$.facets.*.label", contains("Source", "Category", "Study type")))
                .andExpect(
                        jsonPath("$.facets.*.name", contains("source", "category", "study_type")))
                .andExpect(
                        jsonPath(
                                "$.facets[0].values.*.label",
                                contains("UniProtKB reviewed (Swiss-Prot)")))
                .andExpect(
                        jsonPath(
                                "$.facets[0].values.*.value",
                                contains("uniprotkb_reviewed_swissprot")))
                .andExpect(jsonPath("$.facets[0].values.*.count", contains(3)))
                .andExpect(
                        jsonPath("$.facets[1].values.*.label", contains("Interaction", "Pathol")))
                .andExpect(
                        jsonPath("$.facets[1].values.*.value", contains("interaction", "pathol")))
                .andExpect(jsonPath("$.facets[1].values.*.count", contains(3, 2)))
                .andExpect(
                        jsonPath(
                                "$.facets[2].values.*.label",
                                contains("Large scale", "Small scale")))
                .andExpect(
                        jsonPath(
                                "$.facets[2].values.*.value",
                                contains("large_scale", "small_scale")))
                .andExpect(jsonPath("$.facets[2].values.*.count", contains(2, 1)));
    }

    @Test
    void getPublicationsInvalidRequestParamsBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(MAPPED_PROTEIN_PATH + "P12345/publications")
                                .param("facets", "invalid")
                                // TODO: add validation for filter query....
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.url", not(isEmptyOrNullString())))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "Invalid facet name 'invalid'. Expected value can be [source, category, study_type].")));
    }

    @Test
    void getPublicationsInvalidPathParamsBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(MAPPED_PROTEIN_PATH + "INVALID/publications")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.url", not(isEmptyOrNullString())))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "The 'accession' value has invalid format. It should be a valid UniProtKB accession")));
    }

    @Test
    void getPublicationsNotFound() throws Exception {
        // given
        saveEntry(10, "P12309", "P12310", "P12311");

        // when
        ResultActions response =
                mockMvc.perform(
                        get(MAPPED_PROTEIN_PATH + "P99999/publications")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(0)));
    }

    @Test
    void getPublicationsCanPaginateOverTwoPagesResults() throws Exception {
        // given
        IntStream.rangeClosed(10, 16).forEach(i -> saveEntry(i, "P12345", "P123" + i));

        // when first page
        ResultActions response =
                mockMvc.perform(
                        get(MAPPED_PROTEIN_PATH + "P12345/publications")
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("facets", "study_type")
                                .param("size", "5"));

        // then first page
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "7"))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(header().string(HttpHeaders.LINK, containsString("size=5")))
                .andExpect(header().string(HttpHeaders.LINK, containsString("cursor=")))
                .andExpect(jsonPath("$.results.size()", is(5)))
                .andExpect(jsonPath("$.facets.size()", is(1)))
                .andExpect(jsonPath("$.facets[0].values.*.count", contains(7)));

        String linkHeader = response.andReturn().getResponse().getHeader(HttpHeaders.LINK);
        assertThat(linkHeader, notNullValue());

        String cursor = linkHeader.split("\\?")[1].split("&")[1].split("=")[1];
        // when last page
        response =
                mockMvc.perform(
                        get(MAPPED_PROTEIN_PATH + "P12345/publications")
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("cursor", cursor)
                                .param("size", "5"));

        // then last page
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "7"))
                .andExpect(header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(jsonPath("$.results.size()", is(2)));
    }

    private void saveEntry(long pubMedId, String... accessions) {
        System.out.println("Document for PUBMED_ID: " + pubMedId);
        LiteratureDocument document = UniprotKBObjectsForTests.getLiteratureDocument(pubMedId);

        storeManager.saveDocs(DataStoreManager.StoreType.LITERATURE, document);
    }

    private void saveUniprotEntryInStore(String accession, String... pubmedIds) {
        storeClient.saveEntry(
                UniprotKBObjectsForTests.getUniprotEntryForPublication(accession, pubmedIds));
    }
}
