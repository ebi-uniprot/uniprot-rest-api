package org.uniprot.api.support.data;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.api.rest.controller.AbstractSolrStreamControllerIT;
import org.uniprot.api.rest.output.UniProtMediaType;

/**
 * @author sahmad
 * @created 02/02/2021
 */
public abstract class AbstractRDFStreamControllerIT extends AbstractSolrStreamControllerIT {

    protected abstract RestTemplate getRestTemple();

    protected abstract String getSearchAccession();

    protected abstract String getRDFProlog();

    @Test
    void idSuccessRDFContentType() throws Exception {
        when(getRestTemple().getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());
        when(getRestTemple().getForObject(any(), any())).thenReturn(SAMPLE_RDF);
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath())
                        .queryParam("query", "id:" + getSearchAccession())
                        .header(ACCEPT, UniProtMediaType.RDF_MEDIA_TYPE);

        MvcResult response = mockMvc.perform(requestBuilder).andReturn();
        Assertions.assertNotNull(response);

        // then
        mockMvc.perform(asyncDispatch(response))
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.RDF_MEDIA_TYPE_VALUE))
                .andExpect(
                        content()
                                .string(
                                        equalTo(
                                                getRDFProlog()
                                                        + "\n\n"
                                                        + "    <sample>text</sample>\n"
                                                        + "    <anotherSample>text2</anotherSample>\n"
                                                        + "    <someMore>text3</someMore>\n"
                                                        + "\n"
                                                        + "</rdf:RDF>\n")));
    }

    @Test
    void idBadRequestRDFContentType() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getStreamPath()).header(ACCEPT, UniProtMediaType.RDF_MEDIA_TYPE_VALUE);

        ResultActions response = mockMvc.perform(requestBuilder);

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.RDF_MEDIA_TYPE_VALUE));
    }
}
