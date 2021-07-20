package org.uniprot.api.aa.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.uniprot.api.aa.AARestApplication;
import org.uniprot.api.aa.repository.ArbaQueryRepository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.rest.controller.AbstractSolrStreamControllerIT;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.core.unirule.UniRuleEntry;
import org.uniprot.core.unirule.impl.UniRuleEntryBuilderTest;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.unirule.UniRuleDocumentConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.arba.ArbaDocument;
import org.uniprot.store.search.document.unirule.UniRuleDocument;

/**
 * @author sahmad
 * @created 19/07/2021
 */
@ContextConfiguration(
        classes = {DataStoreTestConfig.class, AARestApplication.class, ErrorHandlerConfig.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(ArbaController.class)
@ExtendWith(value = {SpringExtension.class})
class ArbaStreamControllerIT extends AbstractSolrStreamControllerIT {

    @Autowired private ArbaQueryRepository repository;

    @Override
    protected DataStoreManager.StoreType getStoreType() {
        return DataStoreManager.StoreType.ARBA;
    }

    @Override
    protected SolrCollection getSolrCollection() {
        return SolrCollection.arba;
    }

    @Override
    protected SolrQueryRepository getRepository() {
        return repository;
    }

    @Override
    protected String getStreamPath() {
        return "/arba/stream";
    }

    @Override
    protected int saveEntries() {
        int numberOfEntries = 12;
        IntStream.rangeClosed(1, numberOfEntries).forEach(this::saveEntry);
        return numberOfEntries;
    }

    private void saveEntry(int suffix) {
        UniRuleEntry entry = UniRuleEntryBuilderTest.createObject(2);
        UniRuleEntry uniRuleEntry =
                UniRuleControllerITUtils.updateValidValues(
                        entry, suffix, UniRuleControllerITUtils.RuleType.ARBA);
        UniRuleDocumentConverter docConverter = new UniRuleDocumentConverter();
        UniRuleDocument uniRuleDocument = docConverter.convertToDocument(uniRuleEntry);
        // convert the UniRuleDocument to ArbaDocument
        ArbaDocument.ArbaDocumentBuilder arbaDocumentBuilder = ArbaDocument.builder();
        arbaDocumentBuilder.ruleId(uniRuleDocument.getUniRuleId());
        arbaDocumentBuilder.conditionValues(uniRuleDocument.getConditionValues());
        arbaDocumentBuilder.featureTypes(uniRuleDocument.getFeatureTypes());
        arbaDocumentBuilder.keywords(uniRuleDocument.getKeywords());
        arbaDocumentBuilder.geneNames(uniRuleDocument.getGeneNames());
        arbaDocumentBuilder.goTerms(uniRuleDocument.getGoTerms());
        arbaDocumentBuilder.proteinNames(uniRuleDocument.getProteinNames());
        arbaDocumentBuilder.organismNames(uniRuleDocument.getOrganismNames());
        arbaDocumentBuilder.taxonomyNames(uniRuleDocument.getTaxonomyNames());
        arbaDocumentBuilder.commentTypeValues(uniRuleDocument.getCommentTypeValues());
        arbaDocumentBuilder.ruleObj(uniRuleDocument.getUniRuleObj());
        storeManager.saveDocs(DataStoreManager.StoreType.ARBA, arbaDocumentBuilder.build());
    }

    @Test
    void arbaQueryWorks() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "rule_id:ARBA00000002");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();
        Assertions.assertNotNull(response);

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.size()", is(1)))
                .andExpect(jsonPath("$.results.*.uniRuleId", contains("ARBA00000002")));
    }

    @Test
    void arbaQueryEmptyResults() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "condition_value:something");

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
    void arbaSortWorks() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "*")
                        .param("sort", "rule_id desc");

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
                                "$.results.*.uniRuleId",
                                contains(
                                        "ARBA00000012",
                                        "ARBA00000011",
                                        "ARBA00000010",
                                        "ARBA00000009",
                                        "ARBA00000008",
                                        "ARBA00000007",
                                        "ARBA00000006",
                                        "ARBA00000005",
                                        "ARBA00000004",
                                        "ARBA00000003",
                                        "ARBA00000002",
                                        "ARBA00000001")));
    }

    @Test
    void arbaFieldsWorks() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .header(ACCEPT, MediaType.APPLICATION_JSON)
                        .param("query", "*")
                        .param("fields", "rule_id,taxonomic_scope");

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();
        Assertions.assertNotNull(response);

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.results.*.information").doesNotExist())
                .andExpect(jsonPath("$.results.*.uniRuleId").exists())
                .andExpect(jsonPath("$.results.*.mainRule").exists())
                .andExpect(jsonPath("$.results.size()", is(12)));
    }
}
