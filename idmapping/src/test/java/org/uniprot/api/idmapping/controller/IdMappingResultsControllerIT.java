package org.uniprot.api.idmapping.controller;

import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.uniprot.api.idmapping.IdMappingREST;
import org.uniprot.api.idmapping.model.IdMappingJob;
import org.uniprot.api.rest.controller.ControllerITUtils;

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
public class IdMappingResultsControllerIT {
    private static final String ID_MAPPING_RESULT = "/idmapping/results/{jobId}";
    @Autowired private MockMvc mockMvc;
    @Autowired protected JobOperation idMappingResultJobOp;

    @Autowired private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @ParameterizedTest(name = "[{index}] contentType {0}")
    @MethodSource("getContentTypes")
    void testGetResultsAllContentType(MediaType mediaType) throws Exception {
        // when
        IdMappingJob job = idMappingResultJobOp.createAndPutJobInCache();
        MockHttpServletRequestBuilder requestBuilder =
                get(getIdMappingResultPath(), job.getJobId()).header(ACCEPT, mediaType);

        ResultActions response = getMockMvc().perform(requestBuilder);

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, mediaType.toString()))
                .andExpect(content().contentTypeCompatibleWith(mediaType));
    }

    private MockMvc getMockMvc() {
        return this.mockMvc;
    }

    private String getIdMappingResultPath() {
        return ID_MAPPING_RESULT;
    }

    protected Stream<Arguments> getContentTypes() {
        return ControllerITUtils.getContentTypes(
                        getIdMappingResultPath(), requestMappingHandlerMapping)
                .stream()
                .map(Arguments::of);
    }
}
