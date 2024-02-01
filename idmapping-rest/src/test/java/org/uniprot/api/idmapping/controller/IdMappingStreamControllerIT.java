package org.uniprot.api.idmapping.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.uniprot.api.idmapping.IdMappingREST;
import org.uniprot.api.idmapping.common.model.IdMappingJob;
import org.uniprot.api.rest.controller.ControllerITUtils;
import org.uniprot.api.rest.output.header.HttpCommonHeaderConfig;

/**
 * @author sahmad
 * @created 03/03/2021
 */
@ActiveProfiles(profiles = {"offline", "idmapping"})
@ContextConfiguration(classes = {IdMappingREST.class})
@WebMvcTest(IdMappingResultsController.class)
@AutoConfigureWebClient
@ExtendWith(value = {SpringExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdMappingStreamControllerIT extends AbstractIdMappingPIRResultsControllerIT {
    private static final String ID_MAPPING_STREAM = "/idmapping/stream/{jobId}";
    @Autowired private MockMvc mockMvc;

    @ParameterizedTest(name = "[{index}] contentType {0}")
    @MethodSource("getContentTypes")
    void checkStreamedResultsWithAllContentType(MediaType mediaType) throws Exception {
        // when
        IdMappingJob job = idMappingResultJobOp.createAndPutJobInCache();
        MockHttpServletRequestBuilder requestBuilder =
                get(getIdMappingStreamPath(), job.getJobId())
                        .param("download", "true")
                        .header(ACCEPT, mediaType);

        MvcResult mvcResult = getMockMvc().perform(requestBuilder).andReturn();
        ResultActions response = getMockMvc().perform(asyncDispatch(mvcResult));

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        startsWith(
                                                "form-data; name=\"attachment\"; filename=\"idmapping_")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, mediaType.toString()))
                .andExpect(
                        header().string(HttpHeaders.CACHE_CONTROL, HttpCommonHeaderConfig.NO_CACHE))
                .andExpect(content().contentTypeCompatibleWith(mediaType))
                .andExpect(content().string(not(containsString("facets"))));
    }

    @Test
    void testOneLessThanAllowedFromIdsSuccess() { // do nothing
    }

    protected MockMvc getMockMvc() {
        return this.mockMvc;
    }

    @Override
    protected String getIdMappingResultPath() {
        return ID_MAPPING_STREAM;
    }

    private String getIdMappingStreamPath() {
        return ID_MAPPING_STREAM;
    }

    protected Stream<Arguments> getContentTypes() {
        return ControllerITUtils.getContentTypes(
                        getIdMappingStreamPath(), requestMappingHandlerMapping)
                .stream()
                .map(Arguments::of);
    }
}
