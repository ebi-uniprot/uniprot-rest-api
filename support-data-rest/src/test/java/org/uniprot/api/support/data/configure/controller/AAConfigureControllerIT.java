package org.uniprot.api.support.data.configure.controller;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.uniprot.api.support.data.DataStoreTestConfig;
import org.uniprot.api.support.data.SupportDataRestApplication;

/**
 * @author sahmad
 * @created 29/07/2021
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataRestApplication.class})
@WebMvcTest(AAConfigureController.class)
public class AAConfigureControllerIT {
    private static final String CONFIGURE_RESOURCE = "/configure/";

    @Autowired private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {"arba", "unirule"})
    void validResultFields(String ruleType) throws Exception {

        // when
        ResultActions response =
                mockMvc.perform(
                        get(CONFIGURE_RESOURCE + ruleType + "/result-fields")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.size()", is(greaterThan(0))));
    }

    @Test
    void testUniRuleSearchFields() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(CONFIGURE_RESOURCE + "unirule/search-fields")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.size()", is(10)))
                .andExpect(jsonPath("$.[0].label", is("Protein Name [DE]")))
                .andExpect(jsonPath("$.[1].label", is("Gene Name [GN]")))
                .andExpect(jsonPath("$.[2].label", is("Organism [OS]")))
                .andExpect(jsonPath("$.[3].label", is("Taxonomy [OC]")))
                .andExpect(jsonPath("$.[4].label", is("Function")))
                .andExpect(jsonPath("$.[4].itemType", is("group")))
                .andExpect(jsonPath("$.[4].items.size()", is(5)))
                .andExpect(jsonPath("$.[5].label", is("Subcellular location")))
                .andExpect(jsonPath("$.[5].itemType", is("group")))
                .andExpect(jsonPath("$.[5].items.size()", is(1)))
                .andExpect(jsonPath("$.[5].items[0].items.size()", is(2)))
                .andExpect(jsonPath("$.[6].label", is("Expression")))
                .andExpect(jsonPath("$.[6].itemType", is("group")))
                .andExpect(jsonPath("$.[6].items.size()", is(1)))
                .andExpect(jsonPath("$.[7].label", is("Family and Domains")))
                .andExpect(jsonPath("$.[7].itemType", is("group")))
                .andExpect(jsonPath("$.[7].items.size()", is(3)))
                .andExpect(jsonPath("$.[8].label", is("Gene Ontology [GO]")))
                .andExpect(jsonPath("$.[9].label", is("Keyword [KW]")));
    }

    @Test
    void testArbaSearchFields() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(CONFIGURE_RESOURCE + "arba/search-fields")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.size()", is(7)))
                .andExpect(jsonPath("$.[0].label", is("Protein Name [DE]")))
                .andExpect(jsonPath("$.[1].label", is("Organism [OS]")))
                .andExpect(jsonPath("$.[2].label", is("Taxonomy [OC]")))
                .andExpect(jsonPath("$.[3].label", is("Function")))
                .andExpect(jsonPath("$.[3].itemType", is("group")))
                .andExpect(jsonPath("$.[3].items.size()", is(5)))
                .andExpect(jsonPath("$.[4].label", is("Subcellular location")))
                .andExpect(jsonPath("$.[4].itemType", is("group")))
                .andExpect(jsonPath("$.[4].items.size()", is(1)))
                .andExpect(jsonPath("$.[4].items[0].items.size()", is(2)))
                .andExpect(jsonPath("$.[5].label", is("Family and Domains")))
                .andExpect(jsonPath("$.[5].itemType", is("group")))
                .andExpect(jsonPath("$.[5].items.size()", is(3)))
                .andExpect(jsonPath("$.[6].label", is("Keyword [KW]")));
    }
}
