package org.uniprot.api.support.data.configure.controller;

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
                .andExpect(jsonPath("$.fields.length()", is(95)))
                .andExpect(jsonPath("$.fields.[?(@.groupName=='UniProt')]", iterableWithSize(9)))
                .andExpect(
                        jsonPath(
                                "$.fields.[?(@.groupName=='Sequence databases')]",
                                iterableWithSize(3)))
                .andExpect(
                        jsonPath(
                                "$.fields.[?(@.groupName=='3D structure databases')]",
                                iterableWithSize(1)))
                .andExpect(
                        jsonPath(
                                "$.fields.[?(@.groupName=='Protein-protein interaction databases')]",
                                iterableWithSize(4)))
                .andExpect(jsonPath("$.fields.[?(@.groupName=='Chemistry')]", iterableWithSize(4)))
                .andExpect(
                        jsonPath(
                                "$.fields.[?(@.groupName=='Protein family/group databases')]",
                                iterableWithSize(8)))
                .andExpect(
                        jsonPath("$.fields.[?(@.groupName=='PTM databases')]", iterableWithSize(1)))
                .andExpect(
                        jsonPath(
                                "$.fields.[?(@.groupName=='Polymorphism and mutation databases')]",
                                iterableWithSize(2)))
                .andExpect(
                        jsonPath(
                                "$.fields.[?(@.groupName=='2D gel databases')]",
                                iterableWithSize(1)))
                .andExpect(
                        jsonPath(
                                "$.fields.[?(@.groupName=='Proteomic databases')]",
                                iterableWithSize(2)))
                .andExpect(
                        jsonPath(
                                "$.fields.[?(@.groupName=='Protocols and materials databases')]",
                                iterableWithSize(1)))
                .andExpect(
                        jsonPath(
                                "$.fields.[?(@.groupName=='Genome annotation databases')]",
                                iterableWithSize(12)))
                .andExpect(
                        jsonPath(
                                "$.fields.[?(@.groupName=='Organism-specific databases')]",
                                iterableWithSize(32)))
                .andExpect(
                        jsonPath(
                                "$.fields.[?(@.groupName=='Phylogenomic databases')]",
                                iterableWithSize(6)))
                .andExpect(
                        jsonPath(
                                "$.fields.[?(@.groupName=='Enzyme and pathway databases')]",
                                iterableWithSize(4)))
                .andExpect(jsonPath("$.fields.[?(@.groupName=='Other')]", iterableWithSize(3)))
                .andExpect(
                        jsonPath(
                                "$.fields.[?(@.groupName=='Gene expression databases')]",
                                iterableWithSize(1)))
                .andExpect(
                        jsonPath(
                                "$.fields.[?(@.groupName=='Sequence databases')]",
                                iterableWithSize(3)))
                .andExpect(jsonPath("$.fields.[?(@.ruleId==1)]", iterableWithSize(1)))
                .andExpect(jsonPath("$.fields.[?(@.ruleId==2)]", iterableWithSize(1)))
                .andExpect(jsonPath("$.fields.[?(@.ruleId==3)]", iterableWithSize(1)))
                .andExpect(jsonPath("$.fields.[?(@.ruleId==4)]", iterableWithSize(1)))
                .andExpect(jsonPath("$.fields.[?(@.ruleId==5)]", iterableWithSize(1)))
                .andExpect(jsonPath("$.fields.[?(@.ruleId==6)]", iterableWithSize(1)))
                .andExpect(jsonPath("$.fields.[?(@.ruleId==7)]", iterableWithSize(87)))
                .andExpect(jsonPath("$.fields.[?(@.ruleId>=8)]", iterableWithSize(0)))
                .andExpect(jsonPath("$.fields.[?(@.ruleId<=0)]", iterableWithSize(0)))
                .andExpect(jsonPath("$.fields.[?(@.from==true)]", iterableWithSize(93)))
                .andExpect(jsonPath("$.fields.[?(@.from==false)]", iterableWithSize(2)))
                .andExpect(jsonPath("$.fields.[?(@.to==true)]", iterableWithSize(94)))
                .andExpect(jsonPath("$.fields.[?(@.to==false)]", iterableWithSize(1)))
                .andExpect(jsonPath("$.rules.length()", is(7)))
                .andExpect(jsonPath("$.rules.[?(@.taxonId==false)]", iterableWithSize(6)))
                .andExpect(jsonPath("$.rules.[?(@.taxonId==true)]", iterableWithSize(1)))
                .andExpect(jsonPath("$.rules.*.ruleId", containsInAnyOrder(1, 2, 3, 4, 5, 6, 7)));
    }
}
