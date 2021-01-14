package org.uniprot.api.uniprotkb.controller;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.uniprot.api.uniprotkb.UniProtKBObjectsForTests;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.repository.DataStoreTestConfig;
import org.uniprot.api.uniprotkb.repository.search.impl.LiteratureRepository;
import org.uniprot.api.uniprotkb.repository.search.impl.PublicationRepository;
import org.uniprot.api.uniprotkb.repository.store.UniProtKBStoreClient;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.uniprot.mockers.PublicationDocumentMocker;
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
class UniProtKBPublicationControllerIT {

    private static final String MAPPED_PROTEIN_PATH = "/uniprotkb/accession/";
    private static final String ACCESSION = "P12312";
    private static final List<String> VALID_CATEGORIES =
            asList(
                    "Expression",
                    "Family & Domains",
                    "Function",
                    "Interaction",
                    "Names",
                    "Pathology & Biotech",
                    "PTM / Processing",
                    "Sequences",
                    "Subcellular Location",
                    "Structure");

    @Autowired private LiteratureRepository literatureRepository;
    @Autowired private PublicationRepository publicationRepository;

    @Autowired private UniProtKBStoreClient storeClient;

    @Autowired private MockMvc mockMvc;

    @RegisterExtension static DataStoreManager storeManager = new DataStoreManager();

    @BeforeAll
    void initSolrAndInjectItInTheRepository() {
        storeManager.addSolrClient(
                DataStoreManager.StoreType.LITERATURE, SolrCollection.literature);
        storeManager.addStore(DataStoreManager.StoreType.UNIPROT, storeClient);

        storeManager.addSolrClient(
                DataStoreManager.StoreType.PUBLICATION, SolrCollection.publication);
        ReflectionTestUtils.setField(
                literatureRepository,
                "solrClient",
                storeManager.getSolrClient(DataStoreManager.StoreType.LITERATURE));
        ReflectionTestUtils.setField(
                publicationRepository,
                "solrClient",
                storeManager.getSolrClient(DataStoreManager.StoreType.PUBLICATION));
    }

    @BeforeEach
    void cleanData() {
        storeManager.cleanSolr(DataStoreManager.StoreType.LITERATURE);
        storeManager.cleanSolr(DataStoreManager.StoreType.PUBLICATION);
        storeManager.cleanStore(DataStoreManager.StoreType.UNIPROT);
    }

    @Test
    void getPublicationsReturnSuccess() throws Exception {
        // given
        saveEntry(10, "P12309", "P12310");
        saveEntry(11, "P12310", "P12311");
        saveEntry(12, "P12311", ACCESSION);
        saveEntry(13, ACCESSION, "P12313");
        saveEntry(14, "P12313", "P12314");

        saveUniProtEntryInStore(ACCESSION, "11");

        // when
        ResultActions response =
                mockMvc.perform(
                        get(MAPPED_PROTEIN_PATH + ACCESSION + "/publications")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(2)))
                .andExpect(
                        jsonPath("$.results[0].citation.citationCrossReferences[0].id", is("12")))
                .andExpect(
                        jsonPath(
                                "$.results[0].citation.citationType",
                                is("UniProt indexed literatures")))
                .andExpect(jsonPath("$.results[0].citation.title", is("title 12")))
                .andExpect(
                        jsonPath("$.results[*].citation.title", contains("title 12", "title 13")))
                .andExpect(
                        jsonPath(
                                "$.results[*].references[*].sourceCategories[*]",
                                everyItem(in(VALID_CATEGORIES))))
                .andExpect(
                        jsonPath(
                                "$.results[*].references[*].source.source",
                                contains(
                                        "source P12312",
                                        "source P12312",
                                        "source P12312",
                                        "source P12312",
                                        "source P12312",
                                        "source P12312")))
                .andExpect(jsonPath("$.results[0].statistics.reviewedProteinCount", is(10)))
                .andExpect(jsonPath("$.results[0].statistics.unreviewedProteinCount", is(20)))
                .andExpect(
                        jsonPath(
                                "$.results[0].statistics.computationallyMappedProteinCount",
                                is(30)))
                .andExpect(jsonPath("$.results[0].statistics.communityMappedProteinCount", is(40)));
    }

    @Test
    void getPublicationsWithFacetReturnSuccess() throws Exception {
        // given
        saveEntry(10, "P12309", "P12310");
        saveEntry(11, "P12310", "P12311");
        saveEntry(12, "P12311", ACCESSION);
        saveEntry(13, ACCESSION, "P12313");
        saveEntry(14, "P12313", "P12314");

        saveUniProtEntryInStore(ACCESSION, "10", "11");

        // when
        ResultActions response =
                mockMvc.perform(
                        get(MAPPED_PROTEIN_PATH + ACCESSION + "/publications")
                                .param("facets", "types,categories,is_large_scale")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(2)))
                .andExpect(
                        jsonPath("$.facets.*.label", contains("Source", "Category", "Study type")))
                .andExpect(
                        jsonPath(
                                "$.facets.*.name",
                                contains("types", "categories", "is_large_scale")))
                .andExpect(jsonPath("$.facets[0].values[0].label", is("Computationally mapped")))
                .andExpect(jsonPath("$.facets[0].values.*.count", contains(2)))
                .andExpect(jsonPath("$.facets[1].values.*.value", everyItem(in(VALID_CATEGORIES))))
                .andExpect(
                        jsonPath(
                                "$.facets[1].values.*.count",
                                hasSize(lessThanOrEqualTo(VALID_CATEGORIES.size()))))
                .andExpect(
                        jsonPath(
                                "$.facets[2].values.*.label",
                                everyItem(in(asList("Small scale", "Large scale")))))
                .andExpect(
                        jsonPath(
                                "$.facets[2].values.*.value",
                                everyItem(in(asList("true", "false")))));
    }

    @Test
    void getPublicationsWithFacetFiltersSuccess() throws Exception {
        // given
        saveEntry(10, "P12309", "P12310", "P12311");
        saveEntry(11, "P12310", "P12311", ACCESSION);
        saveEntry(12, "P12311", ACCESSION, "P12313");
        saveEntry(13, ACCESSION, "P12313", "P12314");
        saveEntry(14, "P12313", "P12314", "P12315");

        saveUniProtEntryInStore(ACCESSION, "10", "11");

        // when
        ResultActions response =
                mockMvc.perform(
                        get(MAPPED_PROTEIN_PATH + ACCESSION + "/publications")
                                .param("facets", "types,categories,is_large_scale")
                                .param("query", "categories:Interaction")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(3)))
                .andExpect(
                        jsonPath("$.facets.*.label", contains("Source", "Category", "Study type")))
                .andExpect(
                        jsonPath(
                                "$.facets.*.name",
                                contains("types", "categories", "is_large_scale")))
                .andExpect(
                        jsonPath("$.facets[0].values.*.label", contains("Computationally mapped")))
                .andExpect(jsonPath("$.facets[0].values.*.value", contains("1")))
                .andExpect(jsonPath("$.facets[0].values.*.count", contains(3)))
                .andExpect(jsonPath("$.facets[1].values.*.value", hasItem("Interaction")));
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
                                        "Invalid facet name 'invalid'. Expected value can be [types, categories, is_large_scale].")));
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
                                .param("facets", "is_large_scale")
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
                .andExpect(jsonPath("$.facets.size()", is(1)));

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

    private static final AtomicInteger REFERENCE_NUMBER_COUNT = new AtomicInteger();

    private void saveEntry(long pubMedId, String... accessions) {
        System.out.println("Document for PUBMED_ID: " + pubMedId);
        LiteratureDocument document = UniProtKBObjectsForTests.getLiteratureDocument(pubMedId);

        storeManager.saveDocs(DataStoreManager.StoreType.LITERATURE, document);

        for (String accession : accessions) {
            storeManager.saveDocs(
                    DataStoreManager.StoreType.PUBLICATION,
                    PublicationDocumentMocker.createWithAccAndPubMed(
                            accession, pubMedId, REFERENCE_NUMBER_COUNT.getAndIncrement()));
        }
    }

    private void saveUniProtEntryInStore(String accession, String... pubmedIds) {
        storeClient.saveEntry(
                UniProtKBObjectsForTests.getUniprotEntryForPublication(accession, pubmedIds));
    }
}
