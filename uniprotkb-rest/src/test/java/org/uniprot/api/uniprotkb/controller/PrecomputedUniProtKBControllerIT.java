package org.uniprot.api.uniprotkb.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.converter.ConverterConstants;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.common.repository.search.PrecomputedAnnotationRepository;
import org.uniprot.api.uniprotkb.common.repository.store.precomputed.PrecomputedAnnotationStoreClient;
import org.uniprot.api.uniprotkb.common.service.precomputed.PrecomputedUniProtKBEntryService;
import org.uniprot.api.uniprotkb.common.service.precomputed.ProteomeTaxonomyResolver;
import org.uniprot.core.flatfile.parser.UniProtParser;
import org.uniprot.core.flatfile.parser.impl.SupportingDataMapImpl;
import org.uniprot.core.flatfile.parser.impl.aaentry.AAUniProtParser;
import org.uniprot.core.impl.SequenceBuilder;
import org.uniprot.core.uniprotkb.ProteinExistence;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.UniProtKBEntryType;
import org.uniprot.core.uniprotkb.impl.EntryAuditBuilder;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.core.uniprotkb.taxonomy.impl.OrganismBuilder;
import org.uniprot.store.datastore.voldemort.uniprot.VoldemortInMemoryUniprotEntryStore;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.document.precomputed.PrecomputedAnnotationDocument;

@ContextConfiguration(classes = {UniProtKBREST.class})
@ActiveProfiles(profiles = "offline")
@AutoConfigureWebClient
@WebMvcTest(PrecomputedUniProtKBController.class)
@ExtendWith(value = {SpringExtension.class, MockitoExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PrecomputedUniProtKBControllerIT {
    @RegisterExtension static DataStoreManager storeManager = new DataStoreManager();

    private static final String ACCESSION = "P12345";
    private static final String LINK_HEADER = "Link";
    private static final String PRECOMPUTED_SEARCH_PATH = "/uniprotkb/precomputed/search";
    private static final String UNIPARC_ID = "UPI0000001866";
    private static final String TAXONOMY_ID = "61156";
    private static final String INVALID_UPI_MESSAGE =
            "The 'upi' value has invalid format. It should be a valid UniParc UPI";
    private static final String INVALID_TAXONOMY_ID_MESSAGE =
            "The taxonomy id value should be a number";
    private static final String INTERNAL_SERVER_ERROR_MESSAGE = "Internal server error";
    private static final String PRECOMPUTED_SEQUENCE =
            "MSADKELKFLVVDDFSTMRRIVRNLLKELGFNNVEEAEDGVDALNKLQAGGYGFVIS";

    private PrecomputedAnnotationStoreClient storeClient;
    private String precomputedEntryId = "UPI0000001866-61156";
    private UniProtKBEntry precomputedEntry;
    private UniProtKBEntry accessionEntry;

    @Autowired private MockMvc mockMvc;
    @MockBean private PrecomputedAnnotationRepository repository;
    @MockBean private ProteomeTaxonomyResolver proteomeTaxonomyResolver;

    @SpyBean private PrecomputedUniProtKBEntryService precomputedUniProtKBEntryService;

    @BeforeAll
    void init() throws IOException {
        DataStoreManager dsm = storeManager;
        storeClient =
                new PrecomputedAnnotationStoreClient(
                        VoldemortInMemoryUniprotEntryStore.getInstance("avro-uniprot"));
        dsm.addStore(DataStoreManager.StoreType.PRECOMPUTED_ANNOTATION, storeClient);

        precomputedEntry = createPrecomputedEntry();
        accessionEntry =
                new UniProtKBEntryBuilder(ACCESSION, "PRECOMP_HUMAN", UniProtKBEntryType.TREMBL)
                        .build();
    }

    @BeforeEach
    void cleanData() {
        DataStoreManager dsm = storeManager;
        dsm.cleanStore(DataStoreManager.StoreType.PRECOMPUTED_ANNOTATION);
        dsm.saveToStore(
                DataStoreManager.StoreType.PRECOMPUTED_ANNOTATION,
                precomputedEntry,
                accessionEntry);
    }

    @ParameterizedTest
    @MethodSource("getPrecomputedEntrySuccessContentTypes")
    void getPrecomputedEntrySuccessContentTypes(
            MediaType contentType, List<ResultMatcher> resultMatchers) throws Exception {
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(
                                        "/uniprotkb/precomputed/" + UNIPARC_ID + "/" + TAXONOMY_ID)
                                .header(HttpHeaders.ACCEPT, contentType.toString()));

        ResultActions resultActions =
                response.andDo(MockMvcResultHandlers.log())
                        .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                        .andExpect(
                                MockMvcResultMatchers.header()
                                        .string(HttpHeaders.CONTENT_TYPE, contentType.toString()));
        for (ResultMatcher resultMatcher : resultMatchers) {
            resultActions.andExpect(resultMatcher);
        }
    }

    @ParameterizedTest
    @MethodSource("getPrecomputedEntryBadRequestContentTypes")
    void getPrecomputedEntryBadRequestContentTypes(
            MediaType contentType, List<ResultMatcher> resultMatchers) throws Exception {
        String invalidUPI = "UPI1234";
        String invalidTaxon = "taxonId";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(
                                        "/uniprotkb/precomputed/" + invalidUPI + "/" + invalidTaxon)
                                .header(HttpHeaders.ACCEPT, contentType.toString()));

        ResultActions resultActions =
                response.andDo(MockMvcResultHandlers.log())
                        .andExpect(
                                MockMvcResultMatchers.status().is(HttpStatus.BAD_REQUEST.value()))
                        .andExpect(
                                MockMvcResultMatchers.header()
                                        .string(HttpHeaders.CONTENT_TYPE, contentType.toString()));
        for (ResultMatcher resultMatcher : resultMatchers) {
            resultActions.andExpect(resultMatcher);
        }
    }

    @ParameterizedTest
    @MethodSource("getPrecomputedEntryServerErrorContentTypes")
    void getPrecomputedEntryServerErrorContentTypes(
            MediaType contentType, List<ResultMatcher> resultMatchers) throws Exception {
        doThrow(new RuntimeException("Unexpected precomputed service failure"))
                .when(precomputedUniProtKBEntryService)
                .getPrecomputedUniProtKBEntry(UNIPARC_ID, TAXONOMY_ID);

        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(
                                        "/uniprotkb/precomputed/" + UNIPARC_ID + "/" + TAXONOMY_ID)
                                .header(HttpHeaders.ACCEPT, contentType.toString()));

        ResultActions resultActions =
                response.andDo(MockMvcResultHandlers.log())
                        .andExpect(
                                MockMvcResultMatchers.status()
                                        .is(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                        .andExpect(
                                MockMvcResultMatchers.header()
                                        .string(HttpHeaders.CONTENT_TYPE, contentType.toString()));
        for (ResultMatcher resultMatcher : resultMatchers) {
            resultActions.andExpect(resultMatcher);
        }
    }

    @Test
    void searchByProteomeIdSuccess() throws Exception {
        String upId = "UP000000000";
        when(proteomeTaxonomyResolver.findTaxonomyIdByUpId(upId)).thenReturn(TAXONOMY_ID);
        when(repository.searchPage(any(), eq(null)))
                .thenReturn(queryResult(document(ACCESSION, UNIPARC_ID, TAXONOMY_ID)));

        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get("/uniprotkb/precomputed/proteome/" + upId)
                                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE));

        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.length()").value(1))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[0].primaryAccession", Matchers.is(ACCESSION)));

        verify(proteomeTaxonomyResolver).findTaxonomyIdByUpId(upId);
        verify(repository)
                .searchPage(
                        argThat(solrRequest -> "taxonomy_id:61156".equals(solrRequest.getQuery())),
                        eq(null));
    }

    @Test
    void searchByProteomeIdHasNextPageLink() throws Exception {
        String upId = "UP000097203";
        when(proteomeTaxonomyResolver.findTaxonomyIdByUpId(upId)).thenReturn(TAXONOMY_ID);
        when(repository.searchPage(any(), eq(null)))
                .thenReturn(pagedQueryResult(document(ACCESSION, UNIPARC_ID, TAXONOMY_ID)));

        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get("/uniprotkb/precomputed/proteome/" + upId)
                                .param("size", "25")
                                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE));

        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header().string(LINK_HEADER, Matchers.notNullValue()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(LINK_HEADER, Matchers.containsString("size=25")))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(LINK_HEADER, Matchers.containsString("cursor=")))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(
                                        LINK_HEADER,
                                        Matchers.containsString(
                                                "/uniprotkb/precomputed/proteome/" + upId)))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(LINK_HEADER, Matchers.containsString("rel=\"next\"")));
    }

    @Test
    void searchByProteomeIdInvalidUpId() throws Exception {
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get("/uniprotkb/precomputed/proteome/INVALID")
                                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE));

        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.messages.length()").value(1))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.messages[0]")
                                .value(
                                        "The 'upid' value has invalid format. It should be a valid Proteome UPID"));
    }

    @Test
    void searchByProteomeIdTaxonomyNotFound() throws Exception {
        String upId = "UP000097203";
        when(proteomeTaxonomyResolver.findTaxonomyIdByUpId(upId))
                .thenThrow(new ResourceNotFoundException("No proteome found for id: " + upId));

        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get("/uniprotkb/precomputed/proteome/" + upId)
                                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE));

        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.NOT_FOUND.value()));
    }

    private static PrecomputedAnnotationDocument document(
            String accession, String uniparc, String taxonomyId) {
        return PrecomputedAnnotationDocument.builder()
                .accession(accession)
                .uniparc(uniparc)
                .taxonomyId(Integer.valueOf(taxonomyId))
                .build();
    }

    private static QueryResult<PrecomputedAnnotationDocument> queryResult(
            PrecomputedAnnotationDocument... documents) {
        return QueryResult.<PrecomputedAnnotationDocument>builder()
                .content(Stream.of(documents))
                .page(CursorPage.of(null, 25, documents.length))
                .build();
    }

    private static QueryResult<PrecomputedAnnotationDocument> pagedQueryResult(
            PrecomputedAnnotationDocument... documents) {
        CursorPage page = CursorPage.of(null, 25);
        page.setNextCursor("nextCursor");
        page.setTotalElements(50L);
        return QueryResult.<PrecomputedAnnotationDocument>builder()
                .content(Stream.of(documents))
                .page(page)
                .build();
    }

    private UniProtKBEntry createPrecomputedEntry() throws IOException {
        String file = "/it/" + precomputedEntryId + ".txt";
        InputStream inputStream = PrecomputedUniProtKBControllerIT.class.getResourceAsStream(file);
        UniProtParser parser = new AAUniProtParser(new SupportingDataMapImpl(), true);
        UniProtKBEntry entry =
                parser.parse(IOUtils.toString(inputStream, Charset.defaultCharset()));
        return UniProtKBEntryBuilder.from(entry)
                .proteinExistence(ProteinExistence.PREDICTED)
                .organism(
                        new OrganismBuilder()
                                .scientificName("Precomputed annotation organism")
                                .taxonId(Long.parseLong(TAXONOMY_ID))
                                .build())
                .sequence(new SequenceBuilder(PRECOMPUTED_SEQUENCE).build())
                .entryAudit(
                        new EntryAuditBuilder()
                                .firstPublic(LocalDate.of(2024, 1, 1))
                                .lastAnnotationUpdate(LocalDate.of(2024, 1, 1))
                                .lastSequenceUpdate(LocalDate.of(2024, 1, 1))
                                .entryVersion(1)
                                .sequenceVersion(1)
                                .build())
                .build();
    }

    private static Stream<Arguments> getPrecomputedEntrySuccessContentTypes() {
        return Stream.of(
                contentType(
                        MediaType.APPLICATION_JSON,
                        MockMvcResultMatchers.jsonPath(
                                "$.primaryAccession", Matchers.is("UPI0000001866-61156")),
                        MockMvcResultMatchers.jsonPath(
                                "$.proteinExistence", Matchers.is("4: Predicted"))),
                contentType(
                        MediaType.APPLICATION_XML,
                        MockMvcResultMatchers.content()
                                .string(
                                        startsWith(
                                                ConverterConstants.XML_DECLARATION
                                                        + ConverterConstants.UNIPROTKB_XML_SCHEMA)),
                        MockMvcResultMatchers.content()
                                .string(
                                        containsString(
                                                "<accession>UPI0000001866-61156</accession>"))),
                contentType(
                        UniProtMediaType.FF_MEDIA_TYPE,
                        MockMvcResultMatchers.content()
                                .string(containsString("AC   UPI0000001866-61156;")),
                        MockMvcResultMatchers.content()
                                .string(containsString("DE   RecName: Full=ADP/ATP translocase"))),
                contentType(
                        UniProtMediaType.FASTA_MEDIA_TYPE,
                        MockMvcResultMatchers.content()
                                .string(containsString("UPI0000001866-61156")),
                        MockMvcResultMatchers.content()
                                .string(containsString(PRECOMPUTED_SEQUENCE))),
                contentType(
                        UniProtMediaType.GFF_MEDIA_TYPE,
                        MockMvcResultMatchers.content().string(startsWith("##gff-version 3"))),
                contentType(
                        UniProtMediaType.LIST_MEDIA_TYPE,
                        MockMvcResultMatchers.content().string(containsString(precomputedId()))),
                contentType(
                        UniProtMediaType.TSV_MEDIA_TYPE,
                        MockMvcResultMatchers.content()
                                .string(
                                        containsString(
                                                "Entry\tEntry Name\tReviewed\tProtein names\tGene Names\tOrganism\tLength")),
                        MockMvcResultMatchers.content().string(containsString(precomputedId()))),
                contentType(
                        UniProtMediaType.XLS_MEDIA_TYPE,
                        MockMvcResultMatchers.content()
                                .contentType(UniProtMediaType.XLS_MEDIA_TYPE)));
    }

    private static Stream<Arguments> getPrecomputedEntryBadRequestContentTypes() {
        return Stream.of(
                contentType(
                        MediaType.APPLICATION_JSON,
                        MockMvcResultMatchers.jsonPath("$.url", not(emptyOrNullString())),
                        MockMvcResultMatchers.jsonPath("$.messages.length()").value(2),
                        MockMvcResultMatchers.jsonPath(
                                "$.messages",
                                containsInAnyOrder(
                                        INVALID_UPI_MESSAGE, INVALID_TAXONOMY_ID_MESSAGE))),
                contentType(
                        MediaType.APPLICATION_XML,
                        MockMvcResultMatchers.content().string(containsString(INVALID_UPI_MESSAGE)),
                        MockMvcResultMatchers.content()
                                .string(containsString(INVALID_TAXONOMY_ID_MESSAGE))),
                contentType(
                        UniProtMediaType.FF_MEDIA_TYPE,
                        MockMvcResultMatchers.content().string(containsString(INVALID_UPI_MESSAGE)),
                        MockMvcResultMatchers.content()
                                .string(containsString(INVALID_TAXONOMY_ID_MESSAGE))),
                contentType(
                        UniProtMediaType.FASTA_MEDIA_TYPE,
                        MockMvcResultMatchers.content().string(containsString(INVALID_UPI_MESSAGE)),
                        MockMvcResultMatchers.content()
                                .string(containsString(INVALID_TAXONOMY_ID_MESSAGE))),
                contentType(
                        UniProtMediaType.GFF_MEDIA_TYPE,
                        MockMvcResultMatchers.content().string(containsString(INVALID_UPI_MESSAGE)),
                        MockMvcResultMatchers.content()
                                .string(containsString(INVALID_TAXONOMY_ID_MESSAGE))),
                contentType(
                        UniProtMediaType.LIST_MEDIA_TYPE,
                        MockMvcResultMatchers.content().string(containsString(INVALID_UPI_MESSAGE)),
                        MockMvcResultMatchers.content()
                                .string(containsString(INVALID_TAXONOMY_ID_MESSAGE))),
                contentType(
                        UniProtMediaType.TSV_MEDIA_TYPE,
                        MockMvcResultMatchers.content().string(containsString(INVALID_UPI_MESSAGE)),
                        MockMvcResultMatchers.content()
                                .string(containsString(INVALID_TAXONOMY_ID_MESSAGE))),
                contentType(
                        UniProtMediaType.XLS_MEDIA_TYPE,
                        MockMvcResultMatchers.content()
                                .contentType(UniProtMediaType.XLS_MEDIA_TYPE)));
    }

    private static Stream<Arguments> getPrecomputedEntryServerErrorContentTypes() {
        return singleErrorMessageContentTypes(INTERNAL_SERVER_ERROR_MESSAGE);
    }

    private static Stream<Arguments> singleErrorMessageContentTypes(String message) {
        return Stream.of(
                contentType(
                        MediaType.APPLICATION_JSON,
                        MockMvcResultMatchers.jsonPath("$.url", not(emptyOrNullString())),
                        MockMvcResultMatchers.jsonPath("$.messages.*", Matchers.contains(message))),
                contentType(
                        MediaType.APPLICATION_XML,
                        MockMvcResultMatchers.content()
                                .string(containsString("<messages>" + message + "</messages>"))),
                contentType(
                        UniProtMediaType.FF_MEDIA_TYPE,
                        MockMvcResultMatchers.content().string(containsString(message))),
                contentType(
                        UniProtMediaType.FASTA_MEDIA_TYPE,
                        MockMvcResultMatchers.content().string(containsString(message))),
                contentType(
                        UniProtMediaType.GFF_MEDIA_TYPE,
                        MockMvcResultMatchers.content().string(containsString(message))),
                contentType(
                        UniProtMediaType.LIST_MEDIA_TYPE,
                        MockMvcResultMatchers.content().string(containsString(message))),
                contentType(
                        UniProtMediaType.TSV_MEDIA_TYPE,
                        MockMvcResultMatchers.content().string(containsString(message))),
                contentType(
                        UniProtMediaType.XLS_MEDIA_TYPE,
                        MockMvcResultMatchers.content()
                                .contentType(UniProtMediaType.XLS_MEDIA_TYPE)));
    }

    private static Arguments contentType(MediaType contentType, ResultMatcher... resultMatchers) {
        return Arguments.of(contentType, List.of(resultMatchers));
    }

    private static String precomputedId() {
        return UNIPARC_ID + "-" + TAXONOMY_ID;
    }

    @TestConfiguration
    static class LocalTestConfig {
        @Bean
        public PrecomputedAnnotationStoreClient storeClient() {
            return new PrecomputedAnnotationStoreClient(
                    VoldemortInMemoryUniprotEntryStore.getInstance("avro-uniprot"));
        }
    }
}
