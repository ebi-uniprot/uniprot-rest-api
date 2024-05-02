package org.uniprot.api.uniprotkb.controller;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.common.UniProtKBObjectsForTests;
import org.uniprot.api.uniprotkb.common.repository.UniProtKBDataStoreTestConfig;
import org.uniprot.api.uniprotkb.common.repository.search.LiteratureRepository;
import org.uniprot.api.uniprotkb.common.repository.search.PublicationRepository;
import org.uniprot.api.uniprotkb.common.repository.store.UniProtKBStoreClient;
import org.uniprot.core.citation.Submission;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.uniprot.mockers.PublicationDocumentMocker;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.literature.LiteratureDocument;

import lombok.extern.slf4j.Slf4j;

/**
 * @author lgonzales
 * @since 2019-07-10
 */
@Slf4j
@ContextConfiguration(classes = {UniProtKBDataStoreTestConfig.class, UniProtKBREST.class})
@ActiveProfiles(profiles = {"offline"})
@WebMvcTest(UniProtKBPublicationController.class)
@AutoConfigureWebClient
@ExtendWith(value = {SpringExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniProtKBPublicationControllerIT {

    private static final String MAPPED_PROTEIN_PATH = "/uniprotkb/";
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
                        MockMvcRequestBuilders.get(
                                        MAPPED_PROTEIN_PATH + ACCESSION + "/publications")
                                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(2)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[0].citation.citationCrossReferences[0].id", is("12")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[0].citation.citationType",
                                is("UniProt indexed literatures")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[0].citation.title", is("title 12")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[*].citation.title", contains("title 12", "title 13")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[*].references[*].sourceCategories[*]",
                                everyItem(in(VALID_CATEGORIES))))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[*].references[*].source.name",
                                contains(
                                        "source P12312",
                                        "source P12312",
                                        "source P12312",
                                        "source P12312",
                                        "source P12312",
                                        "source P12312")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[0].statistics.reviewedProteinCount", is(10)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[0].statistics.unreviewedProteinCount", is(20)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[0].statistics.computationallyMappedProteinCount",
                                is(30)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[0].statistics.communityMappedProteinCount", is(40)));
    }

    @Test
    void getPublicationsWithSubmissionReturnSuccess() throws Exception {
        // given
        saveEntryWithSubmission(12, ACCESSION);

        // when
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(
                                        MAPPED_PROTEIN_PATH + ACCESSION + "/publications")
                                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(2)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[*].citation.id", contains("CI-F4UM8V2OKRCK4", "12")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[*].citation.citationType",
                                contains("submission", "UniProt indexed literatures")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[*].citation.title",
                                contains("Submission title", "title 12")));
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
                        MockMvcRequestBuilders.get(
                                        MAPPED_PROTEIN_PATH + ACCESSION + "/publications")
                                .param("facets", "types,is_large_scale,categories")
                                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(2)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.facets.*.label", contains("Source", "Study type", "Category")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.facets.*.name",
                                contains("types", "is_large_scale", "categories")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.facets[0].values[0].label", is("Computationally mapped")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.facets[0].values.*.count", contains(2)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.facets[2].values.*.value", everyItem(in(VALID_CATEGORIES))))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.facets[2].values.*.count",
                                hasSize(lessThanOrEqualTo(VALID_CATEGORIES.size()))))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.facets[1].values.*.label",
                                everyItem(in(asList("Small scale", "Large scale")))))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.facets[1].values.*.value",
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
                        MockMvcRequestBuilders.get(
                                        MAPPED_PROTEIN_PATH + ACCESSION + "/publications")
                                .param("facets", "types,is_large_scale,categories")
                                .param("facetFilter", "categories:Interaction")
                                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(3)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.facets.*.label", contains("Source", "Study type", "Category")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.facets.*.name",
                                contains("types", "is_large_scale", "categories")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.facets[0].values.*.label", contains("Computationally mapped")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.facets[0].values.*.value", contains("1")))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.facets[0].values.*.count", contains(3)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.facets[2].values.*.value", hasItem("Interaction")));
    }

    @Test
    void getPublicationsInvalidRequestParamsBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(MAPPED_PROTEIN_PATH + "P12345/publications")
                                .param("facets", "invalid")
                                .param("facetFilter", "invalidQuery:invalidValue")
                                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.url", not(emptyOrNullString())))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "Invalid facet name 'invalid'. Expected value can be [types, categories, is_large_scale].",
                                        "Invalid facet name 'invalidQuery'. Expected value can be [types, categories, is_large_scale].")));
    }

    @Test
    void getPublicationsInvalidPathParamsBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(MAPPED_PROTEIN_PATH + "INVALID/publications")
                                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.url", not(emptyOrNullString())))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
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
                        MockMvcRequestBuilders.get(MAPPED_PROTEIN_PATH + "P99999/publications")
                                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(0)));
    }

    @Test
    void getPublicationsCanPaginateOverTwoPagesResults() throws Exception {
        // given
        IntStream.rangeClosed(10, 16).forEach(i -> saveEntry(i, "P12345", "P123" + i));

        // when first page
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(MAPPED_PROTEIN_PATH + "P12345/publications")
                                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                .param("facets", "is_large_scale")
                                .param("size", "5"));

        // then first page
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpCommonHeaderConfig.X_TOTAL_RESULTS, "7"))
                .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.LINK, containsString("size=5")))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.LINK, containsString("cursor=")))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(5)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.facets.size()", is(1)));

        String linkHeader = response.andReturn().getResponse().getHeader(HttpHeaders.LINK);
        assertThat(linkHeader, notNullValue());

        String cursor = linkHeader.split("\\?")[1].split("&")[1].split("=")[1];
        // when last page
        response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(MAPPED_PROTEIN_PATH + "P12345/publications")
                                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                .param("cursor", cursor)
                                .param("size", "5"));

        // then last page
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpCommonHeaderConfig.X_TOTAL_RESULTS, "7"))
                .andExpect(MockMvcResultMatchers.header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", is(2)));
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

    private void saveEntryWithSubmission(long pubMedId, String accession) {
        System.out.println("Document for PUBMED_ID: " + pubMedId);
        UniProtKBEntry entry =
                UniProtKBObjectsForTests.getUniprotEntryForPublication(
                        accession, String.valueOf(pubMedId));

        entry.getReferences()
                .forEach(
                        reference -> {
                            if (reference.getCitation() instanceof Submission) {
                                LiteratureDocument doc =
                                        UniProtKBObjectsForTests
                                                .getLiteratureDocumentWithSubmission(
                                                        (Submission) reference.getCitation());
                                storeManager.saveDocs(DataStoreManager.StoreType.LITERATURE, doc);
                                storeManager.saveDocs(
                                        DataStoreManager.StoreType.PUBLICATION,
                                        PublicationDocumentMocker.create(accession, doc.getId()));
                            } else {
                                LiteratureDocument doc =
                                        UniProtKBObjectsForTests.getLiteratureDocument(
                                                Long.parseLong(reference.getCitation().getId()));
                                storeManager.saveDocs(DataStoreManager.StoreType.LITERATURE, doc);
                                storeManager.saveDocs(
                                        DataStoreManager.StoreType.PUBLICATION,
                                        PublicationDocumentMocker.create(accession, doc.getId()));
                            }
                        });
        storeClient.saveEntry(entry);
    }

    private void saveUniProtEntryInStore(String accession, String... citationIds) {
        storeClient.saveEntry(
                UniProtKBObjectsForTests.getUniprotEntryForPublication(accession, citationIds));
    }
}
