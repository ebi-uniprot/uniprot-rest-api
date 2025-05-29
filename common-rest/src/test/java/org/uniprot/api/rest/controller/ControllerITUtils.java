package org.uniprot.api.rest.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.uniprot.api.rest.controller.AbstractStreamControllerIT.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.DefaultUriBuilderFactory;
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
            RestTemplate restTemplate, String dataType, String ids) {
        String urlTemplate = "http://localhost/{dataType}/{format}/{ids}";
        String urlRdf = getUrlWithFormat(dataType, "rdf", ids, urlTemplate);
        String urlTtl = getUrlWithFormat(dataType, "ttl", ids, urlTemplate);
        String urlNt = getUrlWithFormat(dataType, "nt", ids, urlTemplate);
        DefaultUriBuilderFactory handler = new DefaultUriBuilderFactory(urlTemplate);
        when(restTemplate.getUriTemplateHandler()).thenReturn(handler);
        // Match by URI.toString()
        when(restTemplate.getForObject(
                        argThat(uri -> uri != null && uri.toString().equals(urlRdf)),
                        eq(String.class)))
                .thenReturn(SAMPLE_RDF);

        when(restTemplate.getForObject(
                        argThat(uri -> uri != null && uri.toString().equals(urlTtl)),
                        eq(String.class)))
                .thenReturn(SAMPLE_TTL);

        when(restTemplate.getForObject(
                        argThat(uri -> uri != null && uri.toString().equals(urlNt)),
                        eq(String.class)))
                .thenReturn(SAMPLE_N_TRIPLES);
    }

    private static String getUrlWithFormat(
            String dataType, String format, String ids, String urlTemplate) {
        return urlTemplate
                .replace("{dataType}", dataType)
                .replace("{format}", format)
                .replace("{ids}", ids);
    }
}
