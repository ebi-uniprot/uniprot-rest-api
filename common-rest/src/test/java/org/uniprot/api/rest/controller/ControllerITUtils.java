package org.uniprot.api.rest.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
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
}
