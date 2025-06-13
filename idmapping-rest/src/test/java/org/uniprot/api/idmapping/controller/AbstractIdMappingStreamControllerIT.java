package org.uniprot.api.idmapping.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.uniprot.api.idmapping.IdMappingREST;
import org.uniprot.api.idmapping.common.IdMappingDataStoreTestConfig;
import org.uniprot.api.idmapping.common.model.IdMappingJob;

/**
 * @author lgonzales
 * @since 10/03/2021
 */
@ContextConfiguration(classes = {IdMappingDataStoreTestConfig.class, IdMappingREST.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstractIdMappingStreamControllerIT extends AbstractIdMappingBasicControllerIT {

    @Override
    protected ResultActions performRequest(MockHttpServletRequestBuilder requestBuilder)
            throws Exception {
        MvcResult response = getMockMvc().perform(requestBuilder).andReturn();
        return getMockMvc().perform(asyncDispatch(response));
    }

    @Test
    void streamInvalidDownloadReturnBadRequest() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache();

        ResultActions response =
                getMockMvc()
                        .perform(
                                get(getIdMappingResultPath(), job.getJobId())
                                        .param("download", "invalid")
                                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.messages.*",
                                containsInAnyOrder(
                                        "The 'download' parameter has invalid format. It should be a boolean true or false.")));
    }

    @Test
    void streamValidDownloadReturnSuccessRequest() throws Exception {
        // when
        IdMappingJob job = getJobOperation().createAndPutJobInCache();

        ResultActions response =
                performRequest(
                        get(getIdMappingResultPath(), job.getJobId())
                                .param("download", "true")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        startsWith(
                                                "form-data; name=\"attachment\"; filename=\"idmapping_")))
                .andExpect(jsonPath("$.results.size()", greaterThan(0)))
                .andExpect(jsonPath("$.facets").doesNotExist());
    }
}
