package org.uniprot.api.support.data.configure.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * @created 15/03/2021
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataRestApplication.class})
@WebMvcTest(IdMappingConfigureController.class)
class IdMappingConfigureControllerIT {
    @Autowired private MockMvc mockMvc;

    @Test
    void canGetIdMappingFields() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get("/configure/idmapping/fields").header(ACCEPT, APPLICATION_JSON_VALUE));
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.groups.length()", is(18)))
                .andExpect(
                        jsonPath(
                                "$.groups.*.groupName",
                                contains(
                                        "UniProt",
                                        "Sequence databases",
                                        "3D structure databases",
                                        "Protein-protein interaction databases",
                                        "Chemistry",
                                        "Protein family/group databases",
                                        "PTM databases",
                                        "Genetic variation databases",
                                        "2D gel databases",
                                        "Proteomic databases",
                                        "Protocols and materials databases",
                                        "Genome annotation databases",
                                        "Organism-specific databases",
                                        "Phylogenomic databases",
                                        "Enzyme and pathway databases",
                                        "Miscellaneous",
                                        "Gene expression databases",
                                        "Family and domain databases")))
                .andExpect(
                        jsonPath(
                                "$.groups.[?(@.groupName=='UniProt')].items.*",
                                iterableWithSize(9)))
                .andExpect(
                        jsonPath(
                                "$.groups.[?(@.groupName=='Sequence databases')].items.*",
                                iterableWithSize(5)))
                .andExpect(
                        jsonPath(
                                "$.groups.[?(@.groupName=='3D structure databases')].items.*",
                                iterableWithSize(1)))
                .andExpect(
                        jsonPath(
                                "$.groups.[?(@.groupName=='Protein-protein interaction databases')].items.*",
                                iterableWithSize(4)))
                .andExpect(
                        jsonPath(
                                "$.groups.[?(@.groupName=='Chemistry')].items.*",
                                iterableWithSize(4)))
                .andExpect(
                        jsonPath(
                                "$.groups.[?(@.groupName=='Protein family/group databases')].items.*",
                                iterableWithSize(7)))
                .andExpect(
                        jsonPath(
                                "$.groups.[?(@.groupName=='PTM databases')].items.*",
                                iterableWithSize(1)))
                .andExpect(
                        jsonPath(
                                "$.groups.[?(@.groupName=='Genetic variation databases')].items.*",
                                iterableWithSize(2)))
                .andExpect(
                        jsonPath(
                                "$.groups.[?(@.groupName=='2D gel databases')].items.*",
                                iterableWithSize(1)))
                .andExpect(
                        jsonPath(
                                "$.groups.[?(@.groupName=='Proteomic databases')].items.*",
                                iterableWithSize(2)))
                .andExpect(
                        jsonPath(
                                "$.groups.[?(@.groupName=='Protocols and materials databases')].items.*",
                                iterableWithSize(1)))
                .andExpect(
                        jsonPath(
                                "$.groups.[?(@.groupName=='Genome annotation databases')].items.*",
                                iterableWithSize(12)))
                .andExpect(
                        jsonPath(
                                "$.groups.[?(@.groupName=='Organism-specific databases')].items.*",
                                iterableWithSize(31)))
                .andExpect(
                        jsonPath(
                                "$.groups.[?(@.groupName=='Phylogenomic databases')].items.*",
                                iterableWithSize(6)))
                .andExpect(
                        jsonPath(
                                "$.groups.[?(@.groupName=='Enzyme and pathway databases')].items.*",
                                iterableWithSize(4)))
                .andExpect(
                        jsonPath(
                                "$.groups.[?(@.groupName=='Miscellaneous')].items.*",
                                iterableWithSize(4)))
                .andExpect(
                        jsonPath(
                                "$.groups.[?(@.groupName=='Gene expression databases')].items.*",
                                iterableWithSize(1)))
                .andExpect(
                        jsonPath(
                                "$.groups.[?(@.groupName=='Family and domain databases')].items.*",
                                iterableWithSize(2)))
                .andExpect(jsonPath("$.groups.*.items.[?(@.ruleId==1)]", iterableWithSize(1)))
                .andExpect(jsonPath("$.groups.*.items.[?(@.ruleId==2)]", iterableWithSize(1)))
                .andExpect(jsonPath("$.groups.*.items.[?(@.ruleId==3)]", iterableWithSize(1)))
                .andExpect(jsonPath("$.groups.*.items.[?(@.ruleId==4)]", iterableWithSize(1)))
                .andExpect(jsonPath("$.groups.*.items.[?(@.ruleId==5)]", iterableWithSize(1)))
                .andExpect(jsonPath("$.groups.*.items.[?(@.ruleId==6)]", iterableWithSize(1)))
                .andExpect(jsonPath("$.groups.*.items.[?(@.ruleId==7)]", iterableWithSize(89)))
                .andExpect(jsonPath("$.groups.*.items.[?(@.ruleId>=8)]", iterableWithSize(0)))
                .andExpect(jsonPath("$.groups.*.items.[?(@.ruleId<=0)]", iterableWithSize(0)))
                .andExpect(jsonPath("$.groups.*.items.[?(@.from==true)]", iterableWithSize(95)))
                .andExpect(jsonPath("$.groups.*.items.[?(@.from==false)]", iterableWithSize(2)))
                .andExpect(jsonPath("$.groups.*.items.[?(@.to==true)]", iterableWithSize(96)))
                .andExpect(jsonPath("$.groups.*.items.[?(@.to==false)]", iterableWithSize(1)))
                .andExpect(jsonPath("$.rules.length()", is(7)))
                .andExpect(jsonPath("$.rules.[?(@.taxonId==false)]", iterableWithSize(6)))
                .andExpect(jsonPath("$.rules.[?(@.taxonId==true)]", iterableWithSize(1)))
                .andExpect(jsonPath("$.rules.[?(@.defaultTo=='UniProtKB')]", iterableWithSize(7)))
                .andExpect(jsonPath("$.rules.*.ruleId", containsInAnyOrder(1, 2, 3, 4, 5, 6, 7)));
    }
}
