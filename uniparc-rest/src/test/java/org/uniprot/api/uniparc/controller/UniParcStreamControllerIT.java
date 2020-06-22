package org.uniprot.api.uniparc.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.uniprot.api.rest.controller.AbstractStreamControllerIT;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.xml.jaxb.uniparc.Entry;
import org.uniprot.core.xml.uniparc.UniParcEntryConverter;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.indexer.uniparc.UniParcDocumentConverter;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniparc.UniParcDocument;

/**
 * @author lgonzales
 * @since 15/06/2020
 */
@Slf4j
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniParcController.class)
@ExtendWith(
        value = {
            SpringExtension.class,
        })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniParcStreamControllerIT extends AbstractStreamControllerIT {

    private static final String UPI_PREF = "UPI0000083A";

    @Autowired private MockMvc mockMvc;

    @Autowired UniProtStoreClient<UniParcEntry> storeClient;

    private static final String streamRequestPath = "/uniparc/stream";

    private final UniParcDocumentConverter documentConverter =
            new UniParcDocumentConverter(TaxonomyRepoMocker.getTaxonomyRepo());

    @BeforeAll
    void saveEntriesInSolrAndStore() throws Exception {
        saveEntries();
    }

    @Test
    void streamCanReturnSuccess() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamRequestPath)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "content:*");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(jsonPath("$.results.size()", is(10)))
                .andExpect(
                        jsonPath(
                                "$.results.*.uniParcId",
                                containsInAnyOrder(
                                        "UPI0000083A10",
                                        "UPI0000083A09",
                                        "UPI0000083A08",
                                        "UPI0000083A07",
                                        "UPI0000083A06",
                                        "UPI0000083A05",
                                        "UPI0000083A04",
                                        "UPI0000083A03",
                                        "UPI0000083A02",
                                        "UPI0000083A01")));
    }

    @Test
    void streamBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(streamRequestPath)
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("query", "invalid:invalid")
                                .param("fields", "invalid,invalid1")
                                .param("sort", "invalid")
                                .param("download", "invalid"));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "'invalid' is not a valid search field",
                                        "Invalid fields parameter value 'invalid'",
                                        "Invalid fields parameter value 'invalid1'",
                                        "Invalid sort parameter format. Expected format fieldName asc|desc.",
                                        "The 'download' parameter has invalid format. It should be a boolean true or false.")));
    }

    @Test
    void streamDownloadCompressedFile() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamRequestPath)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "content:*")
                        .param("download", "true");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        startsWith(
                                                "form-data; name=\"attachment\"; filename=\"uniprot-")))
                .andExpect(jsonPath("$.results.size()", is(10)));
    }

    @Test
    void streamSortWorks() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamRequestPath)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "content:*")
                        .param("sort", "upi desc");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.results.*.uniParcId",
                                contains(
                                        "UPI0000083A10",
                                        "UPI0000083A09",
                                        "UPI0000083A08",
                                        "UPI0000083A07",
                                        "UPI0000083A06",
                                        "UPI0000083A05",
                                        "UPI0000083A04",
                                        "UPI0000083A03",
                                        "UPI0000083A02",
                                        "UPI0000083A01")));
    }

    @ParameterizedTest(name = "[{index}] sort fieldName {0}")
    @MethodSource("getAllSortFields")
    void streamCanSortAllPossibleSortFields(String sortField) throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamRequestPath)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "content:*")
                        .param("sort", sortField + " asc");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(10)));
    }

    @Test
    void streamFields() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamRequestPath)
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "accession:P10006 OR accession:P10005")
                        .param("fields", "gene,organism_id");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.results.*.uniParcId",
                                containsInAnyOrder("UPI0000083A06", "UPI0000083A05")))
                .andExpect(
                        jsonPath(
                                "$.results.*.uniParcCrossReferences.*.properties[?(@.key=='gene_name')].value",
                                containsInAnyOrder(
                                        "geneName05", "geneName06", "geneName05", "geneName06")))
                .andExpect(
                        jsonPath(
                                "$.results.*.taxonomies.*.taxonId",
                                containsInAnyOrder(9606, 10090, 9606, 10090)))
                .andExpect(jsonPath("$.results.*.sequence").doesNotExist())
                .andExpect(jsonPath("$.results.*.sequenceFeatures").doesNotExist());
    }

    @ParameterizedTest(name = "[{index}] contentType {0}")
    @MethodSource("getContentTypes")
    void streamAllContentType(MediaType mediaType) throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(streamRequestPath).header(ACCEPT, mediaType).param("query", "content:*");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, mediaType.toString()))
                .andExpect(content().contentTypeCompatibleWith(mediaType));
    }

    @Override
    protected List<SolrCollection> getSolrCollections() {
        return Collections.singletonList(SolrCollection.uniparc);
    }

    private void saveEntries() throws Exception {
        for (int i = 1; i <= 10; i++) {
            saveEntry(i);
        }
        cloudSolrClient.commit(SolrCollection.uniparc.name());
    }

    private void saveEntry(int i) throws Exception {
        UniParcEntry entry = UniParcControllerITUtils.createEntry(i, UPI_PREF);
        UniParcEntryConverter converter = new UniParcEntryConverter();
        Entry xmlEntry = converter.toXml(entry);
        UniParcDocument doc = documentConverter.convert(xmlEntry);
        cloudSolrClient.addBean(SolrCollection.uniparc.name(), doc);
        storeClient.saveEntry(entry);
    }

    private Stream<Arguments> getAllSortFields() {
        SearchFieldConfig fieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.UNIPARC);
        return fieldConfig.getSearchFieldItems().stream()
                .map(SearchFieldItem::getFieldName)
                .filter(fieldConfig::correspondingSortFieldExists)
                .map(Arguments::of);
    }

    private Stream<Arguments> getContentTypes() {
        return super.getContentTypes(streamRequestPath);
    }
}
