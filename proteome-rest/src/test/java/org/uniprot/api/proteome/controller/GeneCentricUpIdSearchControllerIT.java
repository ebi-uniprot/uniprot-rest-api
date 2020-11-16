package org.uniprot.api.proteome.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.contains;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.uniprot.api.proteome.controller.GeneCentricControllerITUtils.createDocument;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.uniprot.api.proteome.ProteomeRestApplication;
import org.uniprot.api.proteome.repository.GeneCentricQueryRepository;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.genecentric.GeneCentricDocument;

/**
 * @author lgonzales
 * @since 28/10/2020
 */
@ContextConfiguration(
        classes = {
            DataStoreTestConfig.class,
            ProteomeRestApplication.class,
            ErrorHandlerConfig.class
        })
@ActiveProfiles(profiles = "offline")
@WebMvcTest(GeneCentricController.class)
@ExtendWith(value = {SpringExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GeneCentricUpIdSearchControllerIT {

    private static final String UPID_PATH = "/genecentric/upid/";

    private static final String UPID = "UP000000300";
    private static final String UPID_100 = "UP000000100";

    @RegisterExtension protected static DataStoreManager storeManager = new DataStoreManager();

    @Autowired protected MockMvc mockMvc;

    @Autowired private GeneCentricQueryRepository repository;

    @BeforeAll
    void initSolrAndInjectItInTheRepository() {
        storeManager.addSolrClient(
                DataStoreManager.StoreType.GENECENTRIC, SolrCollection.genecentric);
        ReflectionTestUtils.setField(
                repository,
                "solrClient",
                storeManager.getSolrClient(DataStoreManager.StoreType.GENECENTRIC));
        saveEntries();
    }

    @AfterAll
    public void cleanData() {
        storeManager.cleanSolr(DataStoreManager.StoreType.GENECENTRIC);
    }

    private void saveEntries() {
        saveEntry("UP000000100", 100);
        saveEntry("UP000000100", 101);
        saveEntry("UP000000100", 102);
        saveEntry("UP000000200", 200);
        saveEntry("UP000000200", 201);
        saveEntry("UP000000200", 202);
        saveEntry("UP000000300", 300);
        saveEntry("UP000000300", 301);
        saveEntry("UP000000300", 302);
        saveEntry("UP000000300", 303);
        saveEntry("UP000000300", 304);
        saveEntry("UP000000300", 305);
        saveEntry("UP000000300", 306);
    }

    private void saveEntry(String upId, int i) {
        GeneCentricDocument doc = createDocument(upId, i);
        storeManager.saveDocs(DataStoreManager.StoreType.GENECENTRIC, doc);
    }

    @Test
    void validUPIdReturnSuccess() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(UPID_PATH + UPID).header(ACCEPT, MediaType.APPLICATION_JSON);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "7"))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(header().string(HttpHeaders.LINK, containsString("size=5")))
                .andExpect(header().string(HttpHeaders.LINK, containsString("cursor=")))
                .andExpect(jsonPath("$.results.size()", is(5)))
                .andExpect(
                        jsonPath(
                                "$.results.*.canonicalProtein.id",
                                contains("P00300", "P00301", "P00302", "P00303", "P00304")))
                .andExpect(
                        jsonPath("$.results.*.proteomeId", contains(UPID, UPID, UPID, UPID, UPID)))
                .andExpect(jsonPath("$.results.*.relatedProteins.size()", contains(2, 2, 2, 2, 2)));
    }

    @Test
    void invalidUPIdReturnBadRequest() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(UPID_PATH + "INVALID").header(ACCEPT, MediaType.APPLICATION_JSON);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains(
                                        "The 'upid' value has invalid format. It should be a valid Proteome UPID")));
    }

    @Test
    void nonExistentUPIdReturnsEmptyResult() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(UPID_PATH + "UP000000000").header(ACCEPT, MediaType.APPLICATION_JSON);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(0)));
    }

    @Test
    void withFilterFieldsReturnSuccess() throws Exception {
        MockHttpServletRequestBuilder requestBuilder =
                get(UPID_PATH + UPID)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("fields", "accession,proteome_id")
                        .param("size", "2");

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(2)))
                .andExpect(jsonPath("$.results.*.proteomeId", contains(UPID, UPID)))
                .andExpect(
                        jsonPath("$.results.*.canonicalProtein.id", contains("P00300", "P00301")))
                .andExpect(jsonPath("$.results.*.canonicalProtein.sequence").doesNotExist())
                .andExpect(jsonPath("$.results.*.canonicalProtein.geneName").doesNotExist())
                .andExpect(jsonPath("$.results.*.relatedProteins").doesNotExist());
    }

    @Test
    void withInvalidFilterFieldsReturnBadRequest() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(UPID_PATH + UPID)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("fields", "INVALID");

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains("Invalid fields parameter value 'INVALID'")));
    }

    @Test
    void searchWithInvalidPageSizeZeroReturnBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(UPID_PATH + UPID)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("query", "*:*")
                                .param("size", "0"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages.*", contains("'size' must be greater than 0")));
    }

    @Test
    void searchWithInvalidPageSizeBiggerThanMaxReturnBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(UPID_PATH + UPID)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("query", "*:*")
                                .param("size", "" + (SearchRequest.MAX_RESULTS_SIZE + 1)));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                contains("'size' must be less than or equal to 500")));
    }

    @Test
    void searchCanPaginateOverTwoPagesResults() throws Exception {

        // when first page
        ResultActions response =
                mockMvc.perform(
                        get(UPID_PATH + UPID)
                                .header(ACCEPT, APPLICATION_JSON_VALUE)
                                .param("size", "5"));

        // then first page
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "7"))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(header().string(HttpHeaders.LINK, containsString("size=5")))
                .andExpect(header().string(HttpHeaders.LINK, containsString("cursor=")))
                .andExpect(jsonPath("$.results.size()", is(5)));

        String linkHeader = response.andReturn().getResponse().getHeader(HttpHeaders.LINK);
        assertThat(linkHeader, notNullValue());

        String cursor = linkHeader.split("\\?")[1].split("&")[0].split("=")[1];
        // when last page
        response =
                mockMvc.perform(
                        get(UPID_PATH + UPID)
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

    @Test
    void upIdSuccessFastaContentType() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(UPID_PATH + UPID_100).header(ACCEPT, UniProtMediaType.FASTA_MEDIA_TYPE_VALUE);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.FASTA_MEDIA_TYPE_VALUE))
                .andExpect(header().string("X-TotalRecords", "3"))
                .andExpect(header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                ">sp|P00100|uniprotkb_id protein100 OS=Human OX=9606 GN=gene100 PE=1 SV=100\nCCCCC")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                ">sp|P20100|uniprotkb_id aprotein100 OS=Human OX=9606 GN=agene100 PE=1 SV=100\nBBBBB")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                ">tr|P30100|uniprotkb_id twoProtein100 OS=Human OX=9606 GN=twogene100 PE=1 SV=100\nAAAAA")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                ">sp|P00101|uniprotkb_id protein101 OS=Human OX=9606 GN=gene101 PE=1 SV=101\nCCCCC")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                ">sp|P00102|uniprotkb_id protein102 OS=Human OX=9606 GN=gene102 PE=1 SV=102\nCCCCC")));
    }

    @Test
    void upIdBadRequestFastaContentType() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(UPID_PATH + "INVALID").header(ACCEPT, UniProtMediaType.FASTA_MEDIA_TYPE_VALUE);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.FASTA_MEDIA_TYPE_VALUE))
                .andExpect(content().string(emptyString()));
    }

    @Test
    void upIdSuccessXmlContentType() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(UPID_PATH + UPID_100).header(ACCEPT, MediaType.APPLICATION_XML_VALUE);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE))
                .andExpect(header().string("X-TotalRecords", "3"))
                .andExpect(header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(xpath("//GeneCentrics").exists())
                .andExpect(xpath("//GeneCentrics/GeneCentric").nodeCount(3))
                .andExpect(xpath("//GeneCentrics/GeneCentric[1]/proteomeId").string(UPID_100))
                .andExpect(
                        xpath("//GeneCentrics/GeneCentric[1]/canonicalProtein/id").string("P00100"))
                .andExpect(xpath("//GeneCentrics/GeneCentric[1]/relatedProteins").nodeCount(2))
                .andExpect(xpath("//GeneCentrics/GeneCentric[2]/proteomeId").string(UPID_100))
                .andExpect(
                        xpath("//GeneCentrics/GeneCentric[2]/canonicalProtein/id").string("P00101"))
                .andExpect(xpath("//GeneCentrics/GeneCentric[2]/relatedProteins").nodeCount(2))
                .andExpect(xpath("//GeneCentrics/GeneCentric[3]/proteomeId").string(UPID_100))
                .andExpect(
                        xpath("//GeneCentrics/GeneCentric[3]/canonicalProtein/id").string("P00102"))
                .andExpect(xpath("//GeneCentrics/GeneCentric[3]/relatedProteins").nodeCount(2));
    }

    @Test
    void upIdBadRequestXmlContentType() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(UPID_PATH + "INVALID").header(ACCEPT, MediaType.APPLICATION_XML_VALUE);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE))
                .andExpect(xpath("//ErrorInfo").exists())
                .andExpect(
                        xpath("//ErrorInfo/url")
                                .string("http://localhost/genecentric/upid/INVALID"))
                .andExpect(
                        xpath("//ErrorInfo/messages[1]/messages")
                                .string(
                                        "The 'upid' value has invalid format. It should be a valid Proteome UPID"));
    }
}
