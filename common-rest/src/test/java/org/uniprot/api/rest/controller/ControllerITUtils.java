package org.uniprot.api.rest.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.uniprot.api.rest.controller.AbstractStreamControllerIT.*;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;
import org.uniprot.api.rest.controller.param.ContentTypeParam;

/**
 * @author lgonzales
 */
public class ControllerITUtils {

    public static final String CACHE_VALUE = "public, max-age=3600";
    public static final String NO_CACHE_VALUE = "no-cache";

    static void verifyContentTypes(
            String requestPath,
            RequestMappingHandlerMapping requestMappingHandlerMapping,
            List<ContentTypeParam> contentTypes) {
        assertThat(contentTypes, notNullValue());
        assertThat(contentTypes, not(empty()));
        Set<MediaType> mediaTypes =
                contentTypes.stream()
                        .map(ContentTypeParam::getContentType)
                        .collect(Collectors.toSet());

        RequestMappingInfo mappingInfo =
                requestMappingHandlerMapping.getHandlerMethods().keySet().stream()
                        .filter(
                                requestMappingInfo ->
                                        requestMappingInfo
                                                .getPatternsCondition()
                                                .getPatterns()
                                                .stream()
                                                .anyMatch(path -> path.startsWith(requestPath)))
                        .findFirst()
                        .orElse(null);
        assertThat(mappingInfo, notNullValue());
        assertThat(mappingInfo.getProducesCondition().getProducibleMediaTypes(), is(mediaTypes));
    }

    static void verifyIdContentTypes(
            String requestPath,
            RequestMappingHandlerMapping requestMappingHandlerMapping,
            List<ContentTypeParam> contentTypes) {
        assertThat(contentTypes, notNullValue());
        assertThat(contentTypes, not(empty()));
        Set<MediaType> mediaTypes =
                contentTypes.stream()
                        .map(ContentTypeParam::getContentType)
                        .collect(Collectors.toSet());

        RequestMappingInfo mappingInfo =
                requestMappingHandlerMapping.getHandlerMethods().keySet().stream()
                        .filter(
                                requestMappingInfo ->
                                        requestMappingInfo
                                                .getPatternsCondition()
                                                .getPatterns()
                                                .stream()
                                                .anyMatch(path -> path.equals(requestPath)))
                        .findFirst()
                        .orElse(null);
        assertThat(mappingInfo, notNullValue());
        assertThat(mappingInfo.getProducesCondition().getProducibleMediaTypes(), is(mediaTypes));
    }

    public static Set<MediaType> getContentTypes(
            String requestPath, RequestMappingHandlerMapping requestMappingHandlerMapping) {

        RequestMappingInfo mappingInfo =
                requestMappingHandlerMapping.getHandlerMethods().keySet().stream()
                        .filter(
                                requestMappingInfo ->
                                        requestMappingInfo
                                                .getPatternsCondition()
                                                .getPatterns()
                                                .stream()
                                                .anyMatch(path -> path.startsWith(requestPath)))
                        .findFirst()
                        .orElse(null);
        assertThat(mappingInfo, notNullValue());
        return mappingInfo.getProducesCondition().getProducibleMediaTypes();
    }

    public static void mockRestTemplateResponsesForRDFFormats(
            RestTemplate restTemplate, String dataType) {
        DefaultUriBuilderFactory handler = Mockito.mock(DefaultUriBuilderFactory.class);
        when(restTemplate.getUriTemplateHandler()).thenReturn(handler);
        UriBuilder uriBuilder = Mockito.mock(UriBuilder.class);
        lenient().when(handler.builder()).thenReturn(uriBuilder);
        // rdf format
        URI rdfServiceUri = Mockito.mock(URI.class);
        lenient().when(uriBuilder.build(eq(dataType), eq("rdf"), any())).thenReturn(rdfServiceUri);
        when(restTemplate.getForObject(eq(rdfServiceUri), any())).thenReturn(SAMPLE_RDF);
        // ttl
        URI ttlServiceUri = Mockito.mock(URI.class);
        lenient().when(uriBuilder.build(eq(dataType), eq("ttl"), any())).thenReturn(ttlServiceUri);
        when(restTemplate.getForObject(eq(ttlServiceUri), any())).thenReturn(SAMPLE_TTL);

        // nt
        URI ntServiceUri = Mockito.mock(URI.class);
        lenient().when(uriBuilder.build(eq(dataType), eq("nt"), any())).thenReturn(ntServiceUri);
        when(restTemplate.getForObject(eq(ntServiceUri), any())).thenReturn(SAMPLE_N_TRIPLES);
    }
}
