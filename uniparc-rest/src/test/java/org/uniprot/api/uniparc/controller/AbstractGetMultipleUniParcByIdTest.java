package org.uniprot.api.uniparc.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.uniparc.common.repository.search.UniParcQueryRepository;
import org.uniprot.core.util.Utils;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;
import org.uniprot.store.indexer.DataStoreManager;

import lombok.extern.slf4j.Slf4j;

/**
 * @author sahmad
 * @created 13/08/2020
 */
@Slf4j
abstract class AbstractGetMultipleUniParcByIdTest {
    @RegisterExtension static DataStoreManager storeManager = new DataStoreManager();

    @Autowired protected MockMvc mockMvc;

    private static final String UPI_PREF = "UPI0000083C";
    protected static String ACCESSION = "P12301";
    protected static String UNIPARC_ID = "UPI0000283A01";

    @Autowired private UniParcQueryRepository repository;

    protected abstract String getGetByIdEndpoint();

    protected abstract String getSearchValue();

    @Value("${voldemort.uniparc.cross.reference.groupSize:#{null}}")
    private Integer xrefGroupSize;

    @BeforeAll
    void initDataStore() {
        UniParcITUtils.initStoreManager(storeManager, repository);

        // create 5 entries
        IntStream.rangeClosed(1, 5)
                .forEach(i -> UniParcITUtils.saveEntry(storeManager, xrefGroupSize, i));
    }

    @AfterAll
    void cleanUp() {
        storeManager.cleanSolr(DataStoreManager.StoreType.UNIPARC);
        storeManager.close();
    }

    @ParameterizedTest(name = "[{index}] try to get by id with content-type {0}")
    @ValueSource(strings = {MediaType.APPLICATION_PDF_VALUE, UniProtMediaType.OBO_MEDIA_TYPE_VALUE})
    void testGetByIdWithUnsupportedContentTypes(String contentType) throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), getSearchValue())
                                .header(ACCEPT, contentType));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        jsonPath(
                                "$.messages[0]",
                                containsString(
                                        "Invalid request received. Requested media type/format not accepted:")));
    }

    @ParameterizedTest(name = "[{index}] get by id with content-type {0}")
    @ValueSource(
            strings = {
                "",
                MediaType.APPLICATION_JSON_VALUE,
                UniProtMediaType.FASTA_MEDIA_TYPE_VALUE,
                TSV_MEDIA_TYPE_VALUE,
                XLS_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE
            })
    void testGetByIdSupportedContentTypes(String contentType) throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), getSearchValue())
                                .header(ACCEPT, contentType));

        if (Utils.nullOrEmpty(contentType)) {
            contentType = MediaType.APPLICATION_JSON_VALUE;
        }
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, contentType));
    }

    @Test
    void testGetIdWithInvalidReturnedFields() throws Exception {
        String searchVal = getSearchValue();
        // when
        ResultActions response =
                mockMvc.perform(
                        get(getGetByIdEndpoint(), searchVal)
                                .param("fields", "randomField")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.messages", notNullValue()))
                .andExpect(jsonPath("$.messages", iterableWithSize(1)))
                .andExpect(
                        jsonPath(
                                "$.messages[0]",
                                containsString("Invalid fields parameter value ")));
    }

    @ParameterizedTest(name = "[{index}] return for fieldName {0} and paths: {1}")
    @MethodSource("getAllReturnedFields")
    void testGetByIdWithAllAvailableReturnedFields(String name, List<String> paths)
            throws Exception {
        String searchVal = getSearchValue();
        // when
        ResultActions response =
                mockMvc.perform(
                        get(getGetByIdEndpoint(), searchVal)
                                .param("fields", name)
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        ResultActions resultActions =
                response.andDo(log())
                        .andExpect(status().is(HttpStatus.OK.value()))
                        .andExpect(
                                header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE));

        for (String path : paths) {
            String returnFieldValidatePath =
                    getGetByIdEndpoint().contains("accession")
                            ? "$." + path
                            : "$.results[*]." + path;
            log.info("ReturnField:" + name + " Validation Path: " + returnFieldValidatePath);
            resultActions.andExpect(jsonPath(returnFieldValidatePath).hasJsonPath());
        }
    }

    protected static Stream<Arguments> getAllReturnedFields() {
        return ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIPARC)
                .getReturnFields()
                .stream()
                .map(
                        returnField -> {
                            String lightPath =
                                    returnField.getPaths().get(returnField.getPaths().size() - 1);
                            return Arguments.of(
                                    returnField.getName(), Collections.singletonList(lightPath));
                        });
    }

    protected String extractCursor(ResultActions response) {
        return UniParcITUtils.extractCursor(response, 1);
    }
}
