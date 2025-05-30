package org.uniprot.api.rest.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.rest.controller.AbstractStreamControllerIT.SAMPLE_RDF;

import java.net.URI;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;
import org.uniprot.api.rest.output.UniProtMediaType;

/**
 * @author sahmad
 * @created 02/02/2021
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractGetByIdWithTypeExtensionControllerIT
        extends AbstractGetByIdControllerIT {

    protected abstract RestTemplate getRestTemple();

    protected abstract String getSearchAccession();

    protected abstract String getIdRequestPathWithoutPathVariable();

    protected String getRDFType() {
        return getIdRequestPathWithoutPathVariable();
    }

    @BeforeAll
    void init() {
        saveEntry();
    }

    @BeforeEach
    void setUp() {
        ControllerITUtils.mockRestTemplateResponsesForRDFFormats(
                getRestTemple(), getRDFType().split("/")[1]);
    }

    @Test
    void getByIdWithRdfExtensionSuccess() throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(getIdRequestPathWithoutPathVariable() + getSearchAccession() + ".rdf")
                        .header(ACCEPT, UniProtMediaType.RDF_MEDIA_TYPE);

        ResultActions response = getMockMvc().perform(requestBuilder);
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.RDF_MEDIA_TYPE_VALUE))
                .andExpect(content().string(equalTo(SAMPLE_RDF)));
    }

    @Test
    void getByIdWithRdfWithoutLastHeaderFailure() throws Exception {
        // when
        DefaultUriBuilderFactory handler = Mockito.mock(DefaultUriBuilderFactory.class);
        when(getRestTemple().getUriTemplateHandler()).thenReturn(handler);

        UriBuilder uriBuilder = Mockito.mock(UriBuilder.class);
        when(handler.builder()).thenReturn(uriBuilder);

        URI uniProtRdfServiceURI = Mockito.mock(URI.class);
        when(uriBuilder.build(eq(getRDFType().split("/")[1]), eq("rdf"), any()))
                .thenReturn(uniProtRdfServiceURI);
        when(getRestTemple().getForObject(eq(uniProtRdfServiceURI), any()))
                .thenReturn(
                        "<?xml version='1.0' encoding='UTF-8'?>\n"
                                + "<rdf:RDF>\n"
                                + "    <rdf:Description rdf:about=\"P00001\">\n"
                                + "</rdf:RDF>");
        MockHttpServletRequestBuilder requestBuilder =
                get(getIdRequestPathWithoutPathVariable() + getSearchAccession() + ".rdf")
                        .header(ACCEPT, UniProtMediaType.RDF_MEDIA_TYPE);

        ResultActions response = getMockMvc().perform(requestBuilder);
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_TYPE,
                                        UniProtMediaType.RDF_MEDIA_TYPE_VALUE))
                .andExpect(
                        content()
                                .string(
                                        containsString(
                                                "<messages>Unable to find last header : &lt;/owl:Ontology&gt;</messages>")));
    }

    @ParameterizedTest(name = "[{index}] for {0}")
    @MethodSource("getContentTypes")
    void testGetByIdWithTypeExtension(MediaType mediaType) throws Exception {
        // when
        MockHttpServletRequestBuilder requestBuilder =
                get(
                        getIdRequestPathWithoutPathVariable()
                                + getSearchAccession()
                                + "."
                                + UniProtMediaType.getFileExtension(mediaType));

        ResultActions response = getMockMvc().perform(requestBuilder);

        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, mediaType.toString()))
                .andExpect(content().contentTypeCompatibleWith(mediaType));
    }

    protected Stream<Arguments> getContentTypes() {
        return ControllerITUtils.getContentTypes(
                        getIdRequestPath(), getRequestMappingHandlerMapping())
                .stream()
                .map(Arguments::of);
    }
}
