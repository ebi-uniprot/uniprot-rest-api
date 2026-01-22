package org.uniprot.api.support.data.configure.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.stream.Collectors;

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
import org.uniprot.cv.xdb.UniProtDatabaseTypes;

/**
 * @author lgonzales
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {DataStoreTestConfig.class, SupportDataRestApplication.class})
@WebMvcTest(UniProtKBConfigureController.class)
class UniProtKBConfigureControllerIT {

    private static final String BASIC_RESOURCE = "/configure/uniprotkb";

    @Autowired private MockMvc mockMvc;

    @ParameterizedTest(name = "/configure/uniprotkb{0}")
    @ValueSource(
            strings = {
                "/annotation_evidences",
                "/go_evidences",
                "/databases",
                "/evidenceDatabases",
                "/result-fields"
            })
    void canGetResponse(String path) throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(get(BASIC_RESOURCE + path).header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        validateResponse(response);
    }

    @Test
    void canGetUniProtSearchTerms() throws Exception {

        // when
        ResultActions response =
                mockMvc.perform(
                        get(BASIC_RESOURCE + "/search-fields")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        List<String> internalCrossRefs =
                UniProtDatabaseTypes.INSTANCE.getInternalDatabaseDetails().stream()
                        .map(db -> db.getName())
                        .collect(Collectors.toList());
        // then
        validateResponse(response);
        response.andExpect(jsonPath("$.[5].autoComplete", is("/suggester?dict=organism&query=?")))
                .andExpect(
                        jsonPath(
                                "$.[?(@.id=='cross_references')].items.*.items.*.label",
                                everyItem(not(is(in(internalCrossRefs))))))
                .andExpect(jsonPath("$.[?(@.id=='cross_references')].items.size()", contains(21)))
                .andExpect(
                        jsonPath(
                                "$.[?(@.id=='cross_references')].items[0].id",
                                contains("xref_source")))
                .andExpect(
                        jsonPath(
                                "$.[?(@.id=='cross_references')].items[0].term",
                                contains("source")))
                .andExpect(jsonPath("$.[?(@.id=='sequence')].items.size()", contains(19)))
                .andExpect(jsonPath("$.[?(@.id=='sequence')].items[1].id", contains("checksum")));
    }

    @Test
    void canGetAllDatabases() throws Exception {

        // when
        ResultActions response =
                mockMvc.perform(
                        get(BASIC_RESOURCE + "/allDatabases")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));
        // all cross refs including internal ones
        List<String> allCrossRefs =
                UniProtDatabaseTypes.INSTANCE.getUniProtKBDbTypes().stream()
                        .map(db -> db.getName())
                        .collect(Collectors.toList());
        response.andExpect(jsonPath("$.*.idMappingName").doesNotExist())
                .andExpect(jsonPath("$.*.name.*", everyItem(is(oneOf(allCrossRefs)))));

        validateResponse(response);
    }

    private void validateResponse(ResultActions response) throws Exception {
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.size()", is(greaterThan(0))));
    }
}
