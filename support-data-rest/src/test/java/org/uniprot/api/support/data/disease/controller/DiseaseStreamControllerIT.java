package org.uniprot.api.support.data.disease.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

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
import org.uniprot.api.rest.service.RDFPrologs;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.support.data.AbstractRDFStreamControllerIT;
import org.uniprot.api.support.data.DataStoreTestConfig;
import org.uniprot.api.support.data.SupportDataRestApplication;
import org.uniprot.api.support.data.disease.DiseaseSolrDocumentHelper;
import org.uniprot.api.support.data.disease.repository.DiseaseRepository;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.disease.DiseaseDocument;

/**
 * @author sahmad
 * @created 20/01/2021
 */
@ContextConfiguration(
        classes = {
            DataStoreTestConfig.class,
            SupportDataRestApplication.class,
            ErrorHandlerConfig.class
        })
@ActiveProfiles(profiles = "offline")
@WebMvcTest(DiseaseController.class)
@ExtendWith(value = {SpringExtension.class})
class DiseaseStreamControllerIT extends AbstractRDFStreamControllerIT {
    @Autowired private DiseaseRepository repository;

    @MockBean(name = "supportDataRdfRestTemplate")
    private RestTemplate restTemplate;

    private String searchAccession;
    private List<String> allAccessions = new ArrayList<>();

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.DISEASE;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.disease;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected int saveEntries() {
        int numberOfEntries = 12;
        IntStream.rangeClosed(1, numberOfEntries).forEach(this::saveEntry);
        return numberOfEntries;
    }

    @Override
    protected String getStreamPath() {
        return "/diseases/stream";
    }

    @Test
    void diseaseQueryWorks() throws Exception {
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
                .andExpect(jsonPath("$.results[0].id", is(searchAccession)));
    }

    @Test
    void diseaseQFQueryWorks() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "autosomal dominant syndrome");

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
                                "$.results[0].definition",
                                containsString("autosomal dominant syndrome")));
    }

    @Test
    void diseaseQueryEmptyResults() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "id:DI-00000");

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
    void diseaseSortWorks() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "*")
                        .param("sort", "id desc");

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
                                "$.results.*.id",
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
    void diseaseFieldsWorks() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "*")
                        .param("fields", "name,acronym");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();
        Assertions.assertNotNull(response);

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(12)))
                .andExpect(jsonPath("$.results.*.id").exists())
                .andExpect(jsonPath("$.results.*.name").exists())
                .andExpect(jsonPath("$.results.*.acronym").exists())
                .andExpect(jsonPath("$.results.*.definition").doesNotExist());
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
                        content()
                                .string(
                                        containsString(
                                                "Name\tDiseaseEntry ID\tMnemonic\tDescription")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "ZTTK syndrome12\t"
                                                        + searchAccession
                                                        + "\tZTTKS12\tAn autosomal dominant syndrome characterized by intellectual disability, developmental delay, malformations of the cerebral cortex, epilepsy, vision problems, musculo-skeletal abnormalities, and congenital malformations.")));
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
                .andExpect(content().string("Error messages\n'query' is a required parameter"));
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
                .andExpect(content().string(containsString("default-namespace: uniprot:diseases")))
                .andExpect(content().string(containsString("id: " + searchAccession)))
                .andExpect(content().string(containsString("name:")))
                .andExpect(content().string(containsString("synonym:")))
                .andExpect(content().string(containsString("xref:")));
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
                .andExpect(content().string("Error messages\n'query' is a required parameter"));
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
                .andExpect(content().string("Error messages\n'query' is a required parameter"));
    }

    private void saveEntry(long suffix) {
        String accPrefix = "DI-";
        long num = ThreadLocalRandom.current().nextLong(10000, 99999);
        String accession = accPrefix + num;
        searchAccession = accession;
        allAccessions.add(searchAccession);
        Collections.sort(allAccessions, Collections.reverseOrder());
        saveEntry(accession, suffix);
    }

    private void saveEntry(String accession, long suffix) {
        DiseaseDocument document =
                DiseaseSolrDocumentHelper.constructSolrDocument(accession, suffix);
        storeManager.saveDocs(DataStoreManager.StoreType.DISEASE, document);
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
    protected String getRDFProlog() {
        return RDFPrologs.DISEASE_PROLOG;
    }
}
