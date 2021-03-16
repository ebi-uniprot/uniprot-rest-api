package org.uniprot.api.support.data.configure.controller;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()", is(95)))
                .andExpect(jsonPath("$[?(@.groupName=='UniProt')]", iterableWithSize(9)))
                .andExpect(jsonPath("$[?(@.groupName=='Sequence databases')]", iterableWithSize(3)))
                .andExpect(
                        jsonPath(
                                "$[?(@.groupName=='3D structure databases')]", iterableWithSize(1)))
                .andExpect(
                        jsonPath(
                                "$[?(@.groupName=='Protein-protein interaction databases')]",
                                iterableWithSize(4)))
                .andExpect(jsonPath("$[?(@.groupName=='Chemistry')]", iterableWithSize(4)))
                .andExpect(
                        jsonPath(
                                "$[?(@.groupName=='Protein family/group databases')]",
                                iterableWithSize(8)))
                .andExpect(jsonPath("$[?(@.groupName=='PTM databases')]", iterableWithSize(1)))
                .andExpect(
                        jsonPath(
                                "$[?(@.groupName=='Polymorphism and mutation databases')]",
                                iterableWithSize(2)))
                .andExpect(jsonPath("$[?(@.groupName=='2D gel databases')]", iterableWithSize(1)))
                .andExpect(
                        jsonPath("$[?(@.groupName=='Proteomic databases')]", iterableWithSize(2)))
                .andExpect(
                        jsonPath(
                                "$[?(@.groupName=='Protocols and materials databases')]",
                                iterableWithSize(1)))
                .andExpect(
                        jsonPath(
                                "$[?(@.groupName=='Genome annotation databases')]",
                                iterableWithSize(12)))
                .andExpect(
                        jsonPath(
                                "$[?(@.groupName=='Organism-specific databases')]",
                                iterableWithSize(32)))
                .andExpect(
                        jsonPath(
                                "$[?(@.groupName=='Phylogenomic databases')]", iterableWithSize(6)))
                .andExpect(
                        jsonPath(
                                "$[?(@.groupName=='Enzyme and pathway databases')]",
                                iterableWithSize(4)))
                .andExpect(jsonPath("$[?(@.groupName=='Other')]", iterableWithSize(3)))
                .andExpect(
                        jsonPath(
                                "$[?(@.groupName=='Gene expression databases')]",
                                iterableWithSize(1)))
                .andExpect(jsonPath("$[?(@.groupName=='Sequence databases')]", iterableWithSize(3)))
                .andExpect(jsonPath("$[?(@.ruleId==1)]", iterableWithSize(1)))
                .andExpect(jsonPath("$[?(@.ruleId==2)]", iterableWithSize(4)))
                .andExpect(jsonPath("$[?(@.ruleId==3)]", iterableWithSize(1)))
                .andExpect(jsonPath("$[?(@.ruleId==4)]", iterableWithSize(87)))
                .andExpect(jsonPath("$[?(@.ruleId>=5)]", iterableWithSize(0)))
                .andExpect(jsonPath("$[?(@.ruleId<=0)]", iterableWithSize(0)))
                .andExpect(jsonPath("$[?(@.from==true)]", iterableWithSize(93)))
                .andExpect(jsonPath("$[?(@.from==false)]", iterableWithSize(2)))
                .andExpect(jsonPath("$[?(@.to==true)]", iterableWithSize(94)))
                .andExpect(jsonPath("$[?(@.to==false)]", iterableWithSize(1)));
    }
}
