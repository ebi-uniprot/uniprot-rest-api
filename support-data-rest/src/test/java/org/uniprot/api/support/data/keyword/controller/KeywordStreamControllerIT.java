package org.uniprot.api.support.data.keyword.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.service.RdfPrologs;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.support.data.AbstractRdfStreamControllerIT;
import org.uniprot.api.support.data.DataStoreTestConfig;
import org.uniprot.api.support.data.SupportDataRestApplication;
import org.uniprot.api.support.data.keyword.repository.KeywordRepository;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.keyword.KeywordDocument;

/**
 * @author sahmad
 * @created 21/01/2021
 */
@ContextConfiguration(
        classes = {
            DataStoreTestConfig.class,
            SupportDataRestApplication.class,
            ErrorHandlerConfig.class
        })
@ActiveProfiles(profiles = "offline")
@WebMvcTest(KeywordController.class)
@ExtendWith(value = {SpringExtension.class})
class KeywordStreamControllerIT extends AbstractRdfStreamControllerIT {
    @Autowired private KeywordRepository repository;

    @MockBean(name = "supportDataRdfRestTemplate")
    private RestTemplate restTemplate;

    private String searchAccession;
    private List<String> allAccessions = new ArrayList<>();

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.KEYWORD;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.keyword;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    private Set<String> keywords;

    @Override
    protected int saveEntries() {
        this.keywords = new HashSet<>();
        int numberOfEntries = 12;
        LongStream.rangeClosed(1, numberOfEntries).forEach(this::saveEntry);
        return numberOfEntries;
    }

    @Override
    protected String getStreamPath() {
        return "/keywords/stream";
    }

    @Test
    void keywordQueryWorks() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "id:" + searchAccession);

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();
        Assertions.assertNotNull(response);

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results[0].keyword.id", is(searchAccession)));
    }

    @Test
    void keywordQFQueryWorks() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "Definition value");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();
        Assertions.assertNotNull(response);

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(12)))
                .andExpect(jsonPath("$.results[0].definition", containsString("Definition value")));
    }

    @Test
    void keywordQueryEmptyResults() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "id:KW-0000");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();
        Assertions.assertNotNull(response);

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(0)));
    }

    @Test
    void keywordIdSortWorks() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "*")
                        .param("sort", "name desc");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();
        Assertions.assertNotNull(response);

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(12)))
                .andExpect(
                        jsonPath(
                                "$.results.*.keyword.id",
                                contains(
                                        allAccessions.get(0),
                                        allAccessions.get(1),
                                        allAccessions.get(2),
                                        allAccessions.get(3),
                                        allAccessions.get(4),
                                        allAccessions.get(5),
                                        allAccessions.get(6),
                                        allAccessions.get(7),
                                        allAccessions.get(8),
                                        allAccessions.get(9),
                                        allAccessions.get(10),
                                        allAccessions.get(11))));
    }

    @Test
    void keywordFieldsWorks() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "*")
                        .param("fields", "name");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();
        Assertions.assertNotNull(response);

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(12)))
                .andExpect(jsonPath("$.results.*.keyword.id").exists())
                .andExpect(jsonPath("$.results.*.keyword.name").exists())
                .andExpect(jsonPath("$.results.*.description").doesNotExist());
    }

    @Test
    void idSuccessTsvContentType() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .queryParam("query", "id:" + searchAccession)
                        .header(ACCEPT, UniProtMediaType.TSV_MEDIA_TYPE_VALUE);

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();
        Assertions.assertNotNull(response);

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.TSV_MEDIA_TYPE_VALUE))
                .andExpect(
                        content().string(containsString("Keyword ID\tName\tDefinition\tCategory")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                searchAccession
                                                        + "\tmy keyword "
                                                        + searchAccession
                                                        + "\tDefinition value\tLigand")));
    }

    @Test
    void idBadRequestTsvContentType() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath()).header(ACCEPT, UniProtMediaType.TSV_MEDIA_TYPE_VALUE);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.TSV_MEDIA_TYPE_VALUE))
                .andExpect(content().string(is("Error messages\n'query' is a required parameter")));
    }

    @Test
    void idSuccessXlsContentType() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .queryParam("query", "id:" + searchAccession)
                        .header(ACCEPT, UniProtMediaType.XLS_MEDIA_TYPE_VALUE);

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();
        Assertions.assertNotNull(response);

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.XLS_MEDIA_TYPE_VALUE))
                .andExpect(content().contentType(UniProtMediaType.XLS_MEDIA_TYPE));
    }

    @Test
    void iddBadRequestXlsContentType() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath()).header(ACCEPT, UniProtMediaType.XLS_MEDIA_TYPE_VALUE);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.XLS_MEDIA_TYPE_VALUE))
                .andExpect(content().contentType(UniProtMediaType.XLS_MEDIA_TYPE));
    }

    @Test
    void idSuccessOBOContentType() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .queryParam("query", "id:" + searchAccession)
                        .header(ACCEPT, UniProtMediaType.OBO_MEDIA_TYPE_VALUE);

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();
        Assertions.assertNotNull(response);

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.OBO_MEDIA_TYPE_VALUE))
                .andExpect(content().string(containsString("format-version: 1.2")))
                .andExpect(content().string(containsString("date: ")))
                .andExpect(content().string(containsString("default-namespace: uniprot:keywords")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "[Typedef]\n"
                                                        + "id: category\n"
                                                        + "name: category\n"
                                                        + "is_cyclic: false\n\n")))
                .andExpect(content().string(containsString("id: " + searchAccession)))
                .andExpect(content().string(containsString("name:")))
                .andExpect(content().string(containsString("synonym:")))
                .andExpect(content().string(containsString("xref:")))
                .andExpect(content().string(containsString("is_a:")))
                .andExpect(content().string(containsString("relationship:")));
    }

    @Test
    void idBadRequestOBOContentType() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath()).header(ACCEPT, UniProtMediaType.OBO_MEDIA_TYPE_VALUE);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.OBO_MEDIA_TYPE_VALUE))
                .andExpect(content().string(is("Error messages\n'query' is a required parameter")));
    }

    @Test
    void idSuccessListContentType() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .queryParam(
                                "query",
                                "id:" + allAccessions.get(0) + " OR id:" + allAccessions.get(1))
                        .header(ACCEPT, UniProtMediaType.LIST_MEDIA_TYPE_VALUE);

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();
        Assertions.assertNotNull(response);

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.LIST_MEDIA_TYPE_VALUE))
                .andExpect(content().string(containsString(allAccessions.get(0))))
                .andExpect(content().string(containsString(allAccessions.get(1))));
    }

    @Test
    void idBadRequestListContentType() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath()).header(ACCEPT, UniProtMediaType.LIST_MEDIA_TYPE_VALUE);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.LIST_MEDIA_TYPE_VALUE))
                .andExpect(content().string(is("Error messages\n'query' is a required parameter")));
    }

    private void saveEntry(long suffix) {
        String accession = getNextAccession();
        searchAccession = accession;
        allAccessions.add(searchAccession);
        Collections.sort(allAccessions, Collections.reverseOrder());
        saveEntry(accession, suffix);
    }

    private void saveEntry(String accession, long suffix) {
        KeywordDocument document = KeywordITUtils.createSolrDocument(accession, suffix % 2 == 0);
        storeManager.saveDocs(DataStoreManager.StoreType.KEYWORD, document);
    }

    @Override
    protected RestTemplate getRestTemple() {
        return restTemplate;
    }

    @Override
    protected String getSearchAccession() {
        return searchAccession;
    }

    @Override
    protected String getRdfProlog() {
        return RdfPrologs.KEYWORD_PROLOG;
    }

    private String getNextAccession() {
        String accPrefix = "KW-";
        long num = ThreadLocalRandom.current().nextLong(1000, 9999);
        String accession = accPrefix + num;
        if (this.keywords.contains(accession)) {
            return getSearchAccession();
        }
        return accession;
    }
}
