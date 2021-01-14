package org.uniprot.api.support.data.taxonomy.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.extern.slf4j.Slf4j;

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
import org.uniprot.api.support.data.DataStoreTestConfig;
import org.uniprot.api.support.data.SupportDataRestApplication;
import org.uniprot.api.support.data.taxonomy.repository.TaxonomyRepository;
import org.uniprot.core.json.parser.taxonomy.TaxonomyJsonConfig;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.core.taxonomy.impl.TaxonomyEntryBuilder;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author lgonzales
 * @since 18/09/2020
 */
@Slf4j
@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataRestApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(TaxonomyController.class)
@ExtendWith(value = {SpringExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TaxonomyGetIdsControllerIT {

    private static final String TAX_IDS_RESOURCE = "/taxonomy/taxonIds/";
    @Autowired private MockMvc mockMvc;

    @RegisterExtension static DataStoreManager storeManager = new DataStoreManager();

    @Autowired private TaxonomyRepository repository;

    @BeforeAll
    void initSolrAndInjectItInTheRepository() {
        storeManager.addSolrClient(DataStoreManager.StoreType.TAXONOMY, SolrCollection.taxonomy);
        ReflectionTestUtils.setField(
                repository,
                "solrClient",
                storeManager.getSolrClient(DataStoreManager.StoreType.TAXONOMY));
        saveEntries();
    }

    @AfterAll
    void cleanSolrData() {
        storeManager.cleanSolr(DataStoreManager.StoreType.TAXONOMY);
    }

    @Test
    void jsonValidIdsReturnSuccess() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(TAX_IDS_RESOURCE + "9606,9607").header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.taxonId", contains(9606, 9607)))
                .andExpect(
                        jsonPath(
                                "$.results.*.scientificName",
                                contains("scientific", "scientific")));
    }

    @Test
    void tsvValidIdsReturnSuccess() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(TAX_IDS_RESOURCE + "9606,9607").header(ACCEPT, TSV_MEDIA_TYPE_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, TSV_MEDIA_TYPE_VALUE))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "Taxon Id\tMnemonic\tScientific name\tCommon name\tOther Names\tReviewed")))
                .andExpect(content().string(containsString("9606\tmnemonic\tscientific\tcommon")));
    }

    @Test
    void listValidIdsReturnSuccess() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(TAX_IDS_RESOURCE + "9606,9607").header(ACCEPT, LIST_MEDIA_TYPE_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, LIST_MEDIA_TYPE_VALUE))
                .andExpect(content().string(containsString("9606")))
                .andExpect(content().string(containsString("9607")));
    }

    @Test
    void xlsValidIdsReturnSuccess() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(TAX_IDS_RESOURCE + "9606,9607").header(ACCEPT, XLS_MEDIA_TYPE_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, XLS_MEDIA_TYPE_VALUE))
                .andExpect(content().contentType(XLS_MEDIA_TYPE));
    }

    @Test
    void validIdsCanReturnFacetsSuccess() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(TAX_IDS_RESOURCE + "9606,9607,9608")
                                .queryParam("facets", "reference,reviewed")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.facets.*.name", containsInAnyOrder("reference", "reviewed")))
                .andExpect(jsonPath("$.facets.*.values.size()", containsInAnyOrder(2, 2)))
                .andExpect(
                        jsonPath("$.facets.*.values[0].value", containsInAnyOrder("true", "true")))
                .andExpect(jsonPath("$.facets.*.values[0].count", containsInAnyOrder(2, 2)))
                .andExpect(jsonPath("$.results.*.taxonId", contains(9606, 9607, 9608)))
                .andExpect(
                        jsonPath(
                                "$.results.*.scientificName",
                                contains("scientific", "scientific", "scientific")));
    }

    @Test
    void validIdsCanFilterFacetsReturnSuccess() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(TAX_IDS_RESOURCE + "9606,9607,9608")
                                .queryParam("facetFilter", "reference:false")
                                .queryParam("facets", "reference,reviewed")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.facets.*.name", containsInAnyOrder("reference", "reviewed")))
                .andExpect(jsonPath("$.facets.*.values.size()", containsInAnyOrder(1, 1)))
                .andExpect(
                        jsonPath(
                                "$.facets.*.values[0].value", containsInAnyOrder("false", "false")))
                .andExpect(jsonPath("$.facets.*.values[0].count", containsInAnyOrder(1, 1)))
                .andExpect(jsonPath("$.results.*.taxonId", contains(9607)))
                .andExpect(jsonPath("$.results.*.scientificName", contains("scientific")));
    }

    @Test
    void validDownloadParameterReturnSuccess() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(TAX_IDS_RESOURCE + "9606,9607,9608")
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("download", "true"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        startsWith(
                                                "form-data; name=\"attachment\"; filename=\"uniprot-")))
                .andExpect(jsonPath("$.results.size()", is(3)))
                .andExpect(jsonPath("$.results.*.taxonId", contains(9606, 9607, 9608)));
    }

    @Test
    void validFieldsParameterReturnSuccess() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(TAX_IDS_RESOURCE + "9606,9607,9608")
                                .header(ACCEPT, MediaType.APPLICATION_JSON)
                                .param("fields", "common_name"));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(3)))
                .andExpect(jsonPath("$.results.*.taxonId", contains(9606, 9607, 9608)))
                .andExpect(
                        jsonPath("$.results.*.commonName", contains("common", "common", "common")))
                .andExpect(jsonPath("$.results.*.scientificName").doesNotExist());
    }

    @Test
    void validIdsCanPaginateFirstPageReturnSuccess() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(TAX_IDS_RESOURCE + "9606,9607,9608,9609")
                                .queryParam("size", "2")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "4"))
                .andExpect(header().string(HttpHeaders.LINK, notNullValue()))
                .andExpect(header().string(HttpHeaders.LINK, containsString("size=2")))
                .andExpect(header().string(HttpHeaders.LINK, containsString("cursor=")))
                .andExpect(jsonPath("$.results.size()", is(2)));
    }

    @Test
    void validIdsCanPaginateLastPageReturnSuccess() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(TAX_IDS_RESOURCE + "9606,9607,9608,9609")
                                .queryParam("size", "2")
                                .queryParam("cursor", "88d67348nibydp0vbi3hdwfzrgvrngl3jn")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(header().string("X-TotalRecords", "4"))
                .andExpect(header().string(HttpHeaders.LINK, nullValue()))
                .andExpect(jsonPath("$.results.size()", is(2)));
    }

    @Test
    void inexistentIdsReturnEmptyResult() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(TAX_IDS_RESOURCE + "96,97").header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(0)));
    }

    @Test
    void invalidInputReturnBadRequest() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(TAX_IDS_RESOURCE + "INVALID,OTHER")
                                .queryParam("fields", "INVALID1")
                                .queryParam("facets", "INVALID")
                                .queryParam("facetFilter", "INVALID:{0 10)")
                                .queryParam("download", "INVALID")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.url", not(emptyOrNullString())))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "taxonIds value has invalid format. It should be a list of comma separated taxonIds (without spaces).",
                                        "Invalid fields parameter value 'INVALID1'",
                                        "Invalid facet name 'INVALID'. Expected value can be [reference, reviewed, proteome, annotated].",
                                        "The 'download' parameter has invalid format. It should be a boolean true or false.",
                                        "'facetFilter' parameter has an invalid syntax")));
    }

    @Test
    void invalidInputSizeReturnBadRequest() throws Exception {
        // when
        String taxIds =
                IntStream.range(1, 1003).mapToObj(String::valueOf).collect(Collectors.joining(","));
        ResultActions response =
                mockMvc.perform(
                        get(TAX_IDS_RESOURCE + taxIds)
                                .queryParam("fields", "INVALID1")
                                .queryParam("facetFilter", "INVALID:value")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.url", not(emptyOrNullString())))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "Only '1000' ids are allowed in each request. Your request has '1002' ids.",
                                        "Invalid fields parameter value 'INVALID1'",
                                        "Invalid facet name 'INVALID'. Expected value can be [reference, reviewed, proteome, annotated].")));
    }

    private ByteBuffer getTaxonomyBinary(TaxonomyEntry entry) {
        try {
            return ByteBuffer.wrap(
                    TaxonomyJsonConfig.getInstance()
                            .getFullObjectMapper()
                            .writeValueAsBytes(entry));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse TaxonomyEntry to binary json: ", e);
        }
    }

    private void saveEntries() {
        for (int taxId = 9600; taxId < 9610; taxId++) {
            TaxonomyEntryBuilder entryBuilder = new TaxonomyEntryBuilder();
            TaxonomyEntry taxonomyEntry =
                    entryBuilder
                            .taxonId(taxId)
                            .scientificName("scientific")
                            .commonName("common")
                            .mnemonic("mnemonic")
                            .parentId(9000L)
                            .linksSet(Collections.singletonList("link"))
                            .build();

            TaxonomyDocument document =
                    TaxonomyDocument.builder()
                            .id(String.valueOf(taxId))
                            .taxId((long) taxId)
                            .synonym("synonym")
                            .common("common")
                            .scientific("scientific")
                            .reference(taxId % 2 == 0)
                            .reviewed(taxId % 2 == 0)
                            .taxonomyObj(getTaxonomyBinary(taxonomyEntry))
                            .build();

            storeManager.saveDocs(DataStoreManager.StoreType.TAXONOMY, document);
        }
    }
}
