package org.uniprot.api.uniprotkb.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
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
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.repository.DataStoreTestConfig;
import org.uniprot.api.uniprotkb.repository.search.impl.LiteratureRepository;
import org.uniprot.api.uniprotkb.repository.store.UniProtKBStoreClient;
import org.uniprot.core.builder.DBCrossReferenceBuilder;
import org.uniprot.core.citation.Author;
import org.uniprot.core.citation.CitationXrefType;
import org.uniprot.core.citation.SubmissionDatabase;
import org.uniprot.core.citation.builder.JournalArticleBuilder;
import org.uniprot.core.citation.builder.SubmissionBuilder;
import org.uniprot.core.citation.impl.AuthorImpl;
import org.uniprot.core.citation.impl.PublicationDateImpl;
import org.uniprot.core.json.parser.literature.LiteratureJsonConfig;
import org.uniprot.core.literature.LiteratureEntry;
import org.uniprot.core.literature.LiteratureMappedReference;
import org.uniprot.core.literature.LiteratureStoreEntry;
import org.uniprot.core.literature.builder.LiteratureEntryBuilder;
import org.uniprot.core.literature.builder.LiteratureMappedReferenceBuilder;
import org.uniprot.core.literature.builder.LiteratureStoreEntryBuilder;
import org.uniprot.core.uniprot.UniProtAccession;
import org.uniprot.core.uniprot.UniProtEntryType;
import org.uniprot.core.uniprot.UniProtReference;
import org.uniprot.core.uniprot.builder.UniProtAccessionBuilder;
import org.uniprot.core.uniprot.builder.UniProtEntryBuilder;
import org.uniprot.core.uniprot.builder.UniProtReferenceBuilder;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.literature.LiteratureDocument;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author lgonzales
 * @since 2019-07-10
 */
@Slf4j
@ContextConfiguration(classes = {DataStoreTestConfig.class, UniProtKBREST.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniprotKBEntryController.class)
@AutoConfigureWebClient
@ExtendWith(value = {SpringExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniprotKBEntryControllerIT {

    private static final String MAPPED_PROTEIN_PATH = "/uniprotkb/accession/";

    @Autowired private LiteratureRepository repository;

    @Autowired private UniProtKBStoreClient storeClient;

    @Autowired private MockMvc mockMvc;

    @RegisterExtension static DataStoreManager storeManager = new DataStoreManager();

    @BeforeAll
    void initSolrAndInjectItInTheRepository() {
        storeManager.addSolrClient(
                DataStoreManager.StoreType.LITERATURE, SolrCollection.literature);
        SolrTemplate template =
                new SolrTemplate(storeManager.getSolrClient(DataStoreManager.StoreType.LITERATURE));
        template.afterPropertiesSet();
        ReflectionTestUtils.setField(repository, "solrTemplate", template);

        storeManager.addStore(DataStoreManager.StoreType.UNIPROT, storeClient);
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
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(4)))
                .andExpect(jsonPath("$.results.*.literatureEntry.pubmedId", contains(11, 12, 13)))
                .andExpect(
                        jsonPath(
                                "$.results.*.literatureEntry.title",
                                contains("title 11", "title 12", "title 13")))
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
                                        "Swiss-Prot",
                                        "Swiss-Prot",
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
                                .param("facets", "source,category,scale")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(5)))
                .andExpect(jsonPath("$.facets.*.label", contains("Source", "Category", "Scale")))
                .andExpect(jsonPath("$.facets.*.name", contains("source", "category", "scale")))
                .andExpect(
                        jsonPath(
                                "$.facets[0].values.*.value",
                                contains("Swiss-Prot", "Computationally mapped")))
                .andExpect(jsonPath("$.facets[0].values.*.count", contains(3, 2)))
                .andExpect(
                        jsonPath(
                                "$.facets[1].values.*.value",
                                contains("Interaction", "Function", "Pathol")))
                .andExpect(jsonPath("$.facets[1].values.*.count", contains(3, 2, 2)))
                .andExpect(jsonPath("$.facets[2].values.*.value", contains("Small")))
                .andExpect(jsonPath("$.facets[2].values.*.count", contains(5)));
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
                                .param("facets", "source,category,scale")
                                .param("query", "category:Interaction")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(3)))
                .andExpect(jsonPath("$.facets.*.label", contains("Source", "Category", "Scale")))
                .andExpect(jsonPath("$.facets.*.name", contains("source", "category", "scale")))
                .andExpect(jsonPath("$.facets[0].values.*.value", contains("Swiss-Prot")))
                .andExpect(jsonPath("$.facets[0].values.*.count", contains(3)))
                .andExpect(
                        jsonPath("$.facets[1].values.*.value", contains("Interaction", "Pathol")))
                .andExpect(jsonPath("$.facets[1].values.*.count", contains(3, 2)))
                .andExpect(jsonPath("$.facets[2].values.*.value", contains("Small")))
                .andExpect(jsonPath("$.facets[2].values.*.count", contains(3)));
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
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.url", not(isEmptyOrNullString())))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "Invalid facet name 'invalid'. Expected value can be [source, category, scale].")));
    }

    @Test
    void getPublicationsInvalidPathParamsBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(MAPPED_PROTEIN_PATH + "INVALID/publications")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
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
        response.andDo(print())
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
                                .param("facets", "scale")
                                .param("size", "5"));

        // then first page
        response.andDo(print())
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
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "7"))
                .andExpect(header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(jsonPath("$.results.size()", is(2)));
    }

    private void saveEntry(long pubMedId, String... accessions) {
        LiteratureEntry entry =
                new LiteratureEntryBuilder()
                        .pubmedId(pubMedId)
                        .doiId("doi " + pubMedId)
                        .title("title " + pubMedId)
                        .addAuthor(new AuthorImpl("author " + pubMedId))
                        .journal("journal " + pubMedId)
                        .publicationDate(new PublicationDateImpl("2019"))
                        .build();
        LiteratureStoreEntry storeEntry =
                new LiteratureStoreEntryBuilder()
                        .literatureEntry(entry)
                        .literatureMappedReference(getLiteratureReference(accessions))
                        .build();

        System.out.println("Document for PUBMED_ID: " + pubMedId);
        LiteratureDocument document =
                LiteratureDocument.builder()
                        .id(String.valueOf(pubMedId))
                        .doi(entry.getDoiId())
                        .title(entry.getTitle())
                        .author(
                                entry.getAuthors().stream()
                                        .map(Author::getValue)
                                        .collect(Collectors.toSet()))
                        .journal(entry.getJournal().getName())
                        .published(entry.getPublicationDate().getValue())
                        .content(Collections.singleton(String.valueOf(pubMedId)))
                        .mappedProteins(
                                storeEntry.getLiteratureMappedReferences().stream()
                                        .map(LiteratureMappedReference::getUniprotAccession)
                                        .map(UniProtAccession::getValue)
                                        .collect(Collectors.toSet()))
                        .literatureObj(getLiteratureBinary(storeEntry))
                        .build();

        storeManager.saveDocs(DataStoreManager.StoreType.LITERATURE, document);
    }

    private void saveUniprotEntryInStore(String accession, String... pubmedIds) {
        List<UniProtReference> references =
                Arrays.stream(pubmedIds)
                        .map(
                                pubmedId -> {
                                    return new UniProtReferenceBuilder()
                                            .addPositions("Position MUTAGENESIS pathol " + pubmedId)
                                            .addPositions("Position INTERACTION " + pubmedId)
                                            .citation(
                                                    new JournalArticleBuilder()
                                                            .addCitationXrefs(
                                                                    new DBCrossReferenceBuilder<
                                                                                    CitationXrefType>()
                                                                            .databaseType(
                                                                                    CitationXrefType
                                                                                            .PUBMED)
                                                                            .id(pubmedId)
                                                                            .build())
                                                            .build())
                                            .build();
                                })
                        .collect(Collectors.toList());

        references.add(
                new UniProtReferenceBuilder()
                        .addPositions("Position INTERACTION ")
                        .citation(
                                new SubmissionBuilder()
                                        .title("Submission tittle")
                                        .addAuthor("Submission Author")
                                        .submittedToDatabase(SubmissionDatabase.PDB)
                                        .build())
                        .build());

        storeClient.saveEntry(
                new UniProtEntryBuilder()
                        .primaryAccession(new UniProtAccessionBuilder(accession).build())
                        .uniProtId(null)
                        .active()
                        .entryType(UniProtEntryType.SWISSPROT)
                        .references(references)
                        .build());
    }

    private List<LiteratureMappedReference> getLiteratureReference(String... accessions) {
        return Arrays.stream(accessions)
                .map(
                        accession ->
                                new LiteratureMappedReferenceBuilder()
                                        .uniprotAccession(accession)
                                        .source("source " + accession)
                                        .sourceId("source id " + accession)
                                        .addSourceCategory("function")
                                        .annotation("annotation " + accession)
                                        .build())
                .collect(Collectors.toList());
    }

    private ByteBuffer getLiteratureBinary(LiteratureStoreEntry entry) {
        try {
            return ByteBuffer.wrap(
                    LiteratureJsonConfig.getInstance()
                            .getFullObjectMapper()
                            .writeValueAsBytes(entry));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse LiteratureEntry to binary json: ", e);
        }
    }
}
