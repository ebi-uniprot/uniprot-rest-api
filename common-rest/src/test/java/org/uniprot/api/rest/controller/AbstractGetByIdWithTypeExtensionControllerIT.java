package org.uniprot.api.rest.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.uniprot.api.rest.controller.AbstractStreamControllerIT.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
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
        DefaultUriBuilderFactory handler =
                new DefaultUriBuilderFactory("http://localhost/{dataType}/{format}/{ids}");
        when(getRestTemple().getUriTemplateHandler()).thenReturn(handler);
        // Match by URI.toString()
        when(getRestTemple()
                        .getForObject(
                                argThat(
                                        uri ->
                                                uri != null
                                                        && uri.toString()
                                                                .equals(
                                                                        "http://localhost"
                                                                                + getRDFType()
                                                                                + "rdf/"
                                                                                + getSearchAccession())),
                                eq(String.class)))
                .thenReturn(SAMPLE_RDF);

        when(getRestTemple()
                        .getForObject(
                                argThat(
                                        uri ->
                                                uri != null
                                                        && uri.toString()
                                                                .equals(
                                                                        "http://localhost"
                                                                                + getRDFType()
                                                                                + "ttl/"
                                                                                + getSearchAccession())),
                                eq(String.class)))
                .thenReturn(SAMPLE_TTL);

        when(getRestTemple()
                        .getForObject(
                                argThat(
                                        uri ->
                                                uri != null
                                                        && uri.toString()
                                                                .equals(
                                                                        "http://localhost"
                                                                                + getRDFType()
                                                                                + "nt/"
                                                                                + getSearchAccession())),
                                eq(String.class)))
                .thenReturn(SAMPLE_N_TRIPLES);
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
