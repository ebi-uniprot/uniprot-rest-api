package org.uniprot.api.uniprotkb.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.common.repository.search.PrecomputedAnnotationRepository;
import org.uniprot.api.uniprotkb.common.repository.store.precomputed.PrecomputedAnnotationStoreClient;
import org.uniprot.core.flatfile.parser.UniProtParser;
import org.uniprot.core.flatfile.parser.impl.SupportingDataMapImpl;
import org.uniprot.core.flatfile.parser.impl.aaentry.AAUniProtParser;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.UniProtKBEntryType;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
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
    private static final String PRECOMPUTED_SEARCH_PATH = "/uniprotkb/precomputed/search";
    private static final String UNIPARC_ID = "UPI0000001866";
    private static final String TAXONOMY_ID = "61156";

    private PrecomputedAnnotationStoreClient storeClient;
    private String precomputedEntryId = "UPI0000001866-61156";
    private UniProtKBEntry precomputedEntry;
    private UniProtKBEntry accessionEntry;

    @Autowired private MockMvc mockMvc;
    @MockBean private PrecomputedAnnotationRepository repository;

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

    private UniProtKBEntry createPrecomputedEntry() throws IOException {
        String file = "/it/" + precomputedEntryId + ".txt";
        InputStream inputStream = PrecomputedUniProtKBControllerIT.class.getResourceAsStream(file);
        UniProtParser parser = new AAUniProtParser(new SupportingDataMapImpl(), true);
        UniProtKBEntry entry =
                parser.parse(IOUtils.toString(inputStream, Charset.defaultCharset()));
        return entry;
    }

    @Test
    void testGetPrecomputedEntry_success() throws Exception {
        // when
        String[] tokens = precomputedEntryId.split("-");
        String upiId = tokens[0];
        String taxonId = tokens[1];
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(
                                        "/uniprotkb/precomputed/" + upiId + "/" + taxonId)
                                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.primaryAccession", Matchers.is(precomputedEntryId)));
    }

    @Test
    void testGetPrecomputedEntry_notFound() throws Exception {
        String unknownUPI = "UPI0000000000";
        String taxon = "1";
        // when
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(
                                        "/uniprotkb/precomputed/" + unknownUPI + "/" + taxon)
                                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    void testGetPrecomputedEntry_invalidUPIAndTaxon() throws Exception {
        String invalidUPI = "UPI1234";
        String invalidTaxon = "taxonId";
        // when
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(
                                        "/uniprotkb/precomputed/" + invalidUPI + "/" + invalidTaxon)
                                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.messages.length()").value(2))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.messages",
                                containsInAnyOrder(
                                        "The 'upi' value has invalid format. It should be a valid UniParc UPI",
                                        "The taxonomy id value should be a number")));
    }

    @Test
    void testGetPrecomputedEntry_invalidFormat() throws Exception {
        // when
        String[] tokens = precomputedEntryId.split("-");
        String upiId = tokens[0];
        String taxonId = tokens[1];
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(
                                        "/uniprotkb/precomputed/" + upiId + "/" + taxonId)
                                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE));

        // then
        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.messages").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.messages.length()").value(1))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.messages[0]")
                                .value(
                                        "Invalid request received. Requested media type/format not accepted: 'application/xml'."));
    }

    @Test
    void testSearchPrecomputedEntriesByUniparc() throws Exception {
        when(repository.searchPage(
                        argThat(
                                solrRequest ->
                                        solrRequest.getQuery().equals("uniparc:" + UNIPARC_ID)),
                        isNull()))
                .thenReturn(queryResult(document(precomputedEntryId, UNIPARC_ID, TAXONOMY_ID)));

        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(PRECOMPUTED_SEARCH_PATH)
                                .param("uniparc", UNIPARC_ID)
                                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE));

        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", Matchers.is(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[0].primaryAccession", Matchers.is(precomputedEntryId)));
    }

    @Test
    void testSearchPrecomputedEntriesByAccession() throws Exception {
        when(repository.searchPage(
                        argThat(
                                solrRequest ->
                                        solrRequest.getQuery().equals("accession:" + ACCESSION)),
                        isNull()))
                .thenReturn(queryResult(document(ACCESSION, "UPI0000000001", "9606")));

        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(PRECOMPUTED_SEARCH_PATH)
                                .param("accession", ACCESSION)
                                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE));

        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", Matchers.is(1)))
                .andExpect(
                        MockMvcResultMatchers.jsonPath(
                                "$.results[0].primaryAccession", Matchers.is(ACCESSION)));
    }

    @Test
    void testSearchPrecomputedEntriesNotFound() throws Exception {
        when(repository.searchPage(
                        argThat(solrRequest -> solrRequest.getQuery().equals("taxonomy_id:999999")),
                        isNull()))
                .thenReturn(queryResult());

        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(PRECOMPUTED_SEARCH_PATH)
                                .param("taxonomyId", "999999")
                                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE));

        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.OK.value()))
                .andExpect(
                        MockMvcResultMatchers.header()
                                .string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.jsonPath("$.results.size()", Matchers.is(0)));
    }

    @Test
    void testSearchPrecomputedEntriesInvalidUniparc() throws Exception {
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(PRECOMPUTED_SEARCH_PATH)
                                .param("uniparc", "INVALID")
                                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE));

        response.andDo(MockMvcResultHandlers.log())
                .andExpect(MockMvcResultMatchers.status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.messages.length()").value(1))
                .andExpect(
                        MockMvcResultMatchers.jsonPath("$.messages[0]")
                                .value(
                                        "The 'upi' value has invalid format. It should be a valid UniParc UPI"));
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

    @TestConfiguration
    static class LocalTestConfig {
        @Bean
        public PrecomputedAnnotationStoreClient storeClient() {
            return new PrecomputedAnnotationStoreClient(
                    VoldemortInMemoryUniprotEntryStore.getInstance("avro-uniprot"));
        }
    }
}
