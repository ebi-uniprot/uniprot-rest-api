package org.uniprot.api.idmapping.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.uniprot.api.idmapping.controller.response.JobStatus;
import org.uniprot.api.idmapping.controller.utils.JobOperation;
import org.uniprot.api.idmapping.model.IdMappingJob;

import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.uniprot.api.idmapping.model.PredefinedIdMappingStatus.LIMIT_EXCEED_ERROR;

/**
 * @author sahmad
 * @created 06/12/2021
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractIdMappingPIRResultsControllerIT {
    @Value("${id.mapping.max.from.ids.count}")
    protected Integer maxFromIdsAllowed;

    @Value("${id.mapping.max.to.ids.count}")
    protected Integer maxToIdsAllowed;

    @Autowired
    protected JobOperation idMappingResultJobOp;

    @Autowired protected RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Test
    void testWithMoreThanAllowedMappedId() throws Exception {
        // when
        IdMappingJob job = idMappingResultJobOp.createAndPutJobInCacheWithOneToManyMapping(this.maxFromIdsAllowed, JobStatus.FINISHED);
        MockHttpServletRequestBuilder requestBuilder =
                get(getIdMappingResultPath(), job.getJobId()).header(ACCEPT, APPLICATION_JSON_VALUE);

        ResultActions response = getMockMvc().perform(requestBuilder);

        // then
        ResultActions resultActions =
                response.andDo(log())
                        .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
                        .andExpect(
                                jsonPath(
                                        "$.messages.*",
                                        containsInAnyOrder(
                                                "Invalid request received. " + LIMIT_EXCEED_ERROR.getMessage() + this.maxToIdsAllowed)));;
    }

    protected abstract MockMvc getMockMvc();

    protected abstract String getIdMappingResultPath();
}
