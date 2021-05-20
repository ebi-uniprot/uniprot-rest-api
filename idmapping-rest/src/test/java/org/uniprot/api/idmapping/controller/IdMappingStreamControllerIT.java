package org.uniprot.api.idmapping.controller;

import org.hamcrest.Matchers;
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
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.uniprot.api.idmapping.IdMappingREST;
import org.uniprot.api.idmapping.controller.response.JobStatus;
import org.uniprot.api.idmapping.controller.utils.JobOperation;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.rest.controller.ControllerITUtils;

import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author sahmad
 * @created 03/03/2021
 */
@ActiveProfiles(profiles = "offline")
@ContextConfiguration(classes = {IdMappingREST.class})
@WebMvcTest(IdMappingResultsController.class)
@AutoConfigureWebClient
@ExtendWith(value = {SpringExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IdMappingStreamControllerIT {
    private static final String ID_MAPPING_STREAM = "/idmapping/stream/{jobId}";
    @Autowired private MockMvc mockMvc;
    @Autowired protected JobOperation idMappingResultJobOp;

    @Autowired private RequestMappingHandlerMapping requestMappingHandlerMapping;
    
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
                                                "form-data; name=\"attachment\"; filename=\"uniprot-")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, mediaType.toString()))
                .andExpect(content().contentTypeCompatibleWith(mediaType))
                .andExpect(content().string(not(containsString("facets"))));
    }

    private MockMvc getMockMvc() {
        return this.mockMvc;
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
