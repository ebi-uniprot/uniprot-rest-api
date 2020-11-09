package org.uniprot.api.proteome.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.proteome.controller.GeneCentricControllerITUtils.*;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.proteome.ProteomeRestApplication;
import org.uniprot.api.proteome.repository.GeneCentricQueryRepository;
import org.uniprot.api.rest.controller.AbstractSolrStreamControllerIT;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.proteome.GeneCentricDocument;

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
class GeneCentricStreamControllerIT extends AbstractSolrStreamControllerIT {

    @Autowired private GeneCentricQueryRepository repository;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.GENECENTRIC;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.genecentric;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected String getStreamPath() {
        return "/genecentric/stream";
    }

    @Override
    protected int saveEntries() {
        int numberOfEntries = 12;
        IntStream.rangeClosed(1, numberOfEntries).forEach(this::saveEntry);
        return numberOfEntries;
    }

    @Test
    void geneCentricQueryWorks() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "accession:P20012 AND organism_id:9606");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();
        Assertions.assertNotNull(response);

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results.*.canonicalProtein.id", contains("P00012")))
                .andExpect(
                        jsonPath("$.results.*.canonicalProtein.organism.taxonId", contains(9606)))
                .andExpect(
                        jsonPath("$.results.*.relatedProteins.*.id", contains("P20012", "P30012")));
    }

    @Test
    void geneCentricQueryEmptyResults() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "organism_id:9000");

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
    void geneCentricSortWorks() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "*")
                        .param("sort", "accession_id desc");

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
                                "$.results.*.canonicalProtein.id",
                                contains(
                                        "P00012", "P00011", "P00010", "P00009", "P00008", "P00007",
                                        "P00006", "P00005", "P00004", "P00003", "P00002",
                                        "P00001")));
    }

    @Test
    void geneCentricFieldsWorks() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "*")
                        .param("fields", "gene_name,accession");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();
        Assertions.assertNotNull(response);

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.relatedProteins").doesNotExist())
                .andExpect(jsonPath("$.results.*.canonicalProtein.sequence").doesNotExist())
                .andExpect(jsonPath("$.results.size()", is(12)))
                .andExpect(
                        jsonPath(
                                "$.results.*.canonicalProtein.geneName",
                                contains(
                                        "gene001", "gene002", "gene003", "gene004", "gene005",
                                        "gene006", "gene007", "gene008", "gene009", "gene010",
                                        "gene011", "gene012")))
                .andExpect(
                        jsonPath(
                                "$.results.*.canonicalProtein.id",
                                contains(
                                        "P00001", "P00002", "P00003", "P00004", "P00005", "P00006",
                                        "P00007", "P00008", "P00009", "P00010", "P00011",
                                        "P00012")));
    }

    @Test
    void upIdSuccessFastaContentType() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .queryParam("query", "upid:UP000000010")
                        .header(ACCEPT, UniProtMediaType.FASTA_MEDIA_TYPE_VALUE);

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();
        Assertions.assertNotNull(response);

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.FASTA_MEDIA_TYPE_VALUE))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                ">sp|P00010|uniprotkb_id protein010 OS=Human OX=9606 GN=gene010 PE=1 SV=10\nCCCCC")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                ">sp|P20010|uniprotkb_id aprotein010 OS=Human OX=9606 GN=agene010 PE=1 SV=10\nBBBBB")))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                ">tr|P30010|uniprotkb_id twoProtein010 OS=Human OX=9606 GN=twogene010 PE=1 SV=10\nAAAAA")));
    }

    @Test
    void upIdBadRequestFastaContentType() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath()).header(ACCEPT, UniProtMediaType.FASTA_MEDIA_TYPE_VALUE);

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
                get(getStreamPath())
                        .queryParam("query", "upid:UP000000010")
                        .header(ACCEPT, MediaType.APPLICATION_XML_VALUE);

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();
        Assertions.assertNotNull(response);
        
        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE))
                .andExpect(xpath("//GeneCentrics").exists())
                .andExpect(xpath("//GeneCentrics/GeneCentric").nodeCount(1))
                .andExpect(xpath("//GeneCentrics/GeneCentric[1]/proteomeId").string("UP000000010"))
                .andExpect(
                        xpath("//GeneCentrics/GeneCentric[1]/canonicalProtein/id").string("P00010"))
                .andExpect(xpath("//GeneCentrics/GeneCentric[1]/relatedProteins").nodeCount(2));
    }

    @Test
    void upIdBadRequestXmlContentType() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath()).header(ACCEPT, MediaType.APPLICATION_XML_VALUE);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE))
                .andExpect(xpath("//ErrorInfo").exists())
                .andExpect(xpath("//ErrorInfo/url").string("http://localhost/genecentric/stream"))
                .andExpect(
                        xpath("//ErrorInfo/messages[1]/messages")
                                .string("'query' is a required parameter"));
    }

    private void saveEntry(int i) {
        GeneCentricDocument doc = createDocument(i);
        storeManager.saveDocs(DataStoreManager.StoreType.GENECENTRIC, doc);
    }
}
