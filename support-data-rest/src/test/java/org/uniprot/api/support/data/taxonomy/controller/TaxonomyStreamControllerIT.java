package org.uniprot.api.support.data.taxonomy.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.service.RDFPrologs;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.support.data.AbstractRDFStreamControllerIT;
import org.uniprot.api.support.data.DataStoreTestConfig;
import org.uniprot.api.support.data.SupportDataRestApplication;
import org.uniprot.api.support.data.taxonomy.repository.TaxonomyRepository;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;

/**
 * @author sahmad
 * @created 23/01/2021
 */
@ContextConfiguration(
        classes = {
            DataStoreTestConfig.class,
            SupportDataRestApplication.class,
            ErrorHandlerConfig.class
        })
@ActiveProfiles(profiles = "offline")
@WebMvcTest(TaxonomyController.class)
@ExtendWith(value = {SpringExtension.class})
class TaxonomyStreamControllerIT extends AbstractRDFStreamControllerIT {
    @Autowired private TaxonomyRepository repository;

    @Autowired
    @Qualifier("taxonomyRDFRestTemplate")
    private RestTemplate restTemplate;

    private int searchAccession;
    private List<Integer> allAccessions = new ArrayList<>();

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.TAXONOMY;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.taxonomy;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected int saveEntries() {
        int numberOfEntries = 12;
        LongStream.rangeClosed(1, numberOfEntries).forEach(this::saveEntry);
        return numberOfEntries;
    }

    @Override
    protected String getStreamPath() {
        return "/taxonomy/stream";
    }

    @Test
    void taxonomyQueryWorks() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "id:" + searchAccession);

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();
        Assertions.assertNotNull(response);

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results[0].taxonId", is(searchAccession)));
    }

    @Test
    void taxonomyQFQueryWorks() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "common");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();
        Assertions.assertNotNull(response);

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(12)))
                .andExpect(jsonPath("$.results[0].commonName", containsString("common")));
    }

    @Test
    void taxonomyQueryEmptyResults() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "id:0000");

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
    void taxonomyIdSortWorks() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "*")
                        .param("sort", "scientific desc");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();
        Assertions.assertNotNull(response);

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(12)))
                .andExpect(
                        jsonPath(
                                "$.results.*.taxonId",
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
    void taxonomyFieldsWorks() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "*")
                        .param("fields", "mnemonic,rank");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();
        Assertions.assertNotNull(response);

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(12)))
                .andExpect(jsonPath("$.results.*.taxonId").exists())
                .andExpect(jsonPath("$.results.*.mnemonic").exists())
                .andExpect(jsonPath("$.results.*.rank").exists())
                .andExpect(jsonPath("$.results.*.scientificName").doesNotExist())
                .andExpect(jsonPath("$.results.*.commonName").doesNotExist())
                .andExpect(jsonPath("$.results.*.synonyms").doesNotExist())
                .andExpect(jsonPath("$.results.*.parentId").doesNotExist())
                .andExpect(jsonPath("$.results.*.active").doesNotExist())
                .andExpect(jsonPath("$.results.*.otherNames").doesNotExist())
                .andExpect(jsonPath("$.results.*.lineage").doesNotExist())
                .andExpect(jsonPath("$.results.*.strains").doesNotExist())
                .andExpect(jsonPath("$.results.*.hosts").doesNotExist())
                .andExpect(jsonPath("$.results.*.links").doesNotExist())
                .andExpect(jsonPath("$.results.*.statistics").doesNotExist())
                .andExpect(jsonPath("$.results.*.inactiveReason").doesNotExist());
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
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.TSV_MEDIA_TYPE_VALUE))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "Taxon Id\tMnemonic\tScientific name\tCommon name\tOther Names\tReviewed\tRank\tLineage\tParent\tVirus hosts")))
                .andExpect(content().string(containsString(String.valueOf(searchAccession))));
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
                .andExpect(content().string(emptyString()));
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
                .andExpect(content().string(emptyString()));
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
                .andExpect(content().string(containsString(String.valueOf(allAccessions.get(0)))))
                .andExpect(content().string(containsString(String.valueOf(allAccessions.get(1)))));
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
                .andExpect(content().string(emptyString()));
    }

    private void saveEntry(long suffix) {
        int taxonId = ThreadLocalRandom.current().nextInt(1000, 9999);
        searchAccession = taxonId;
        allAccessions.add(searchAccession);
        Collections.sort(allAccessions, Collections.reverseOrder());
        saveEntry(taxonId, suffix);
    }

    private void saveEntry(long taxonId, long suffix) {
        TaxonomyDocument document = TaxonomyITUtils.createSolrDoc(taxonId, suffix % 2 == 0);
        storeManager.saveDocs(DataStoreManager.StoreType.TAXONOMY, document);
    }

    @Override
    protected RestTemplate getRestTemple() {
        return restTemplate;
    }

    @Override
    protected String getSearchAccession() {
        return String.valueOf(searchAccession);
    }

    @Override
    protected String getRDFProlog() {
        return RDFPrologs.TAXONOMY_PROLOG;
    }
}
