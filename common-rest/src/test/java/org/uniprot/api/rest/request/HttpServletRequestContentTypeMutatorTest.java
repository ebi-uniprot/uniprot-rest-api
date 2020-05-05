package org.uniprot.api.rest.request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.ProducesRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.uniprot.api.rest.output.UniProtMediaType;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.USER_AGENT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.FASTA_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.UNKNOWN_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.request.HttpServletRequestContentTypeMutator.FORMAT;

/**
 * Created 10/12/19
 *
 * @author Edd
 */
@Disabled
class HttpServletRequestContentTypeMutatorTest {

    private RequestMappingHandlerMapping handlerMapping;

    @BeforeEach
    void setUp() {
        PatternsRequestCondition patterns = mock(PatternsRequestCondition.class);
        when(patterns.getPatterns())
                .thenReturn(new HashSet<>(Collections.singletonList("/a/b/{d}")));
        RequestMappingInfo requestMappingInfo = mock(RequestMappingInfo.class);
        when(requestMappingInfo.getPatternsCondition()).thenReturn(patterns);
        ProducesRequestCondition produces = mock(ProducesRequestCondition.class);
        when(produces.getProducibleMediaTypes())
                .thenReturn(
                        new HashSet<>(
                                asList(
                                        UniProtMediaType.FASTA_MEDIA_TYPE,
                                        UniProtMediaType.FF_MEDIA_TYPE)));
        when(requestMappingInfo.getProducesCondition()).thenReturn(produces);

        handlerMapping = mock(RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> handlerMap = new HashMap<>();
        handlerMap.put(requestMappingInfo, null);
        when(handlerMapping.getHandlerMethods()).thenReturn(handlerMap);
    }

    @Test
    void requestWithAcceptedMediaTypeHeader_retainsMediaType() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        String uri = "/a/b/ENTITY";
        when(httpRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost" + uri));
        when(httpRequest.getRequestURI()).thenReturn(uri);
        MediaType mediaType = UniProtMediaType.FF_MEDIA_TYPE;
        when(httpRequest.getHeader(ACCEPT)).thenReturn(mediaType.toString());
        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(httpRequest);

        HttpServletRequestContentTypeMutator.mutate(mutableRequest, handlerMapping);
        assertThat(mutableRequest.getHeader(ACCEPT), is(mediaType.toString()));
    }

    @Test
    void requestWithAcceptedExtension_setsMediaType() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        String uri = "/a/b/ENTITY.fasta";
        when(httpRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost" + uri));
        when(httpRequest.getRequestURI()).thenReturn(uri);
        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(httpRequest);

        HttpServletRequestContentTypeMutator.mutate(mutableRequest, handlerMapping);

        assertThat(mutableRequest.getHeader(ACCEPT), is(FASTA_MEDIA_TYPE_VALUE));
    }

    @Test
    void requestWithAcceptedFormatParameter_setsMediaType() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        String uri = "/a/b/ENTITY";
        when(httpRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost" + uri));
        when(httpRequest.getRequestURI()).thenReturn(uri);
        when(httpRequest.getParameter(FORMAT)).thenReturn("fasta");
        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(httpRequest);

        HttpServletRequestContentTypeMutator.mutate(mutableRequest, handlerMapping);

        assertThat(mutableRequest.getHeader(ACCEPT), is(FASTA_MEDIA_TYPE_VALUE));
    }

    @Test
    void requestWithNotAcceptedFormatParameter_setsUnknownMediaTypeAndMessage() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        String uri = "/a/b/ENTITY";
        when(httpRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost" + uri));
        when(httpRequest.getRequestURI()).thenReturn(uri);
        when(httpRequest.getParameter(FORMAT)).thenReturn("obo");
        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(httpRequest);

        HttpServletRequestContentTypeMutator.mutate(mutableRequest, handlerMapping);

        assertThat(mutableRequest.getHeader(ACCEPT), is(UNKNOWN_MEDIA_TYPE_VALUE));
        verify(httpRequest)
                .setAttribute(
                        matches(HttpServletRequestContentTypeMutator.ERROR_MESSAGE_ATTRIBUTE),
                        anyString());
    }

    @Test
    void requestWithUnknownFormatParameter_setsUnknownMediaTypeAndMessage() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        String uri = "/a/b/ENTITY";
        when(httpRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost" + uri));
        when(httpRequest.getRequestURI()).thenReturn(uri);
        when(httpRequest.getParameter(FORMAT)).thenReturn("WRONG");
        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(httpRequest);

        HttpServletRequestContentTypeMutator.mutate(mutableRequest, handlerMapping);

        assertThat(mutableRequest.getHeader(ACCEPT), is(UNKNOWN_MEDIA_TYPE_VALUE));
        verify(httpRequest)
                .setAttribute(
                        matches(HttpServletRequestContentTypeMutator.ERROR_MESSAGE_ATTRIBUTE),
                        anyString());
    }

    @Test
    void requestWithValidButNotAcceptedMediaTypeForPath_setsUnknownMediaTypeAndMessage() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        String uri = "/a/b/ENTITY";
        when(httpRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost" + uri));
        when(httpRequest.getRequestURI()).thenReturn(uri);
        when(httpRequest.getHeader(ACCEPT)).thenReturn(UniProtMediaType.OBO_MEDIA_TYPE.toString());
        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(httpRequest);

        HttpServletRequestContentTypeMutator.mutate(mutableRequest, handlerMapping);

        String header = mutableRequest.getHeader(ACCEPT);
        assertThat(header, is(UNKNOWN_MEDIA_TYPE_VALUE));
        verify(httpRequest)
                .setAttribute(
                        matches(HttpServletRequestContentTypeMutator.ERROR_MESSAGE_ATTRIBUTE),
                        anyString());
    }

    @Test
    void requestWithEmptyAcceptHeader_setsJsonMediaType() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        String uri = "/a/b/ENTITY";
        when(httpRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost" + uri));
        when(httpRequest.getRequestURI()).thenReturn(uri);
        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(httpRequest);

        HttpServletRequestContentTypeMutator.mutate(mutableRequest, handlerMapping);

        assertThat(mutableRequest.getHeader(ACCEPT), is(APPLICATION_JSON_VALUE));
    }

    @Test
    void requestWithRandomAcceptHeaderAndIsBrowser_setsJsonMediaType() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        String uri = "/a/b/ENTITY";
        when(httpRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost" + uri));
        when(httpRequest.getRequestURI()).thenReturn(uri);
        when(httpRequest.getHeader(ACCEPT)).thenReturn("anything/at-all");
        when(httpRequest.getHeader(USER_AGENT))
                .thenReturn(
                        "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:75.0) Gecko/20100101 Firefox/75.0");

        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(httpRequest);

        HttpServletRequestContentTypeMutator.mutate(mutableRequest, handlerMapping);

        assertThat(mutableRequest.getHeader(ACCEPT), is(APPLICATION_JSON_VALUE));
    }

    @Test
    void requestThatMatchesNoPath_isUntouched() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        String uri = "/a/b/c/d";

        when(httpRequest.getHeader(ACCEPT)).thenReturn(APPLICATION_JSON_VALUE);
        when(httpRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost" + uri));
        when(httpRequest.getRequestURI()).thenReturn(uri);
        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(httpRequest);

        HttpServletRequestContentTypeMutator.mutate(mutableRequest, handlerMapping);

        assertThat(mutableRequest.getHeader(ACCEPT), is(APPLICATION_JSON_VALUE));
        verify(httpRequest, times(0)).setAttribute(anyString(), anyString());
    }

    @Test
    void correctPathIsFoundForRequest() {
        List<String> paths = HttpServletRequestContentTypeMutator.RESOURCE_PATH_2_MEDIA_TYPES_KEYS;
        paths.add("/w/x/{id}");
        paths.add("/w/{id}/y");
        paths.add("/w/x/y");
        paths.add("/w/y/d");
        HttpServletRequestContentTypeMutator.orderKeysSoPathVariablesLast(paths);

        assertThat(
                HttpServletRequestContentTypeMutator.getMatchingPathPattern("/w/x/y"),
                is("/w/x/y"));

        assertThat(
                HttpServletRequestContentTypeMutator.getMatchingPathPattern("/w/x/y2"),
                is("/w/x/{id}"));

        assertThat(
                HttpServletRequestContentTypeMutator.getMatchingPathPattern("/w/x/y.txt"),
                is("/w/x/{id}"));

        assertThat(
                HttpServletRequestContentTypeMutator.getMatchingPathPattern("/X"), is(nullValue()));

        assertThat(
                HttpServletRequestContentTypeMutator.getMatchingPathPattern("/w/y/d"),
                is("/w/y/d"));

        assertThat(
                HttpServletRequestContentTypeMutator.getMatchingPathPattern(
                        "/w/anything.txt-thing;hello/y"),
                is("/w/{id}/y"));
    }

    @Test
    void pathOrderingIsCorrect() {
        List<String> paths = new ArrayList<>();
        paths.add("/w/x/{y}");
        paths.add("/w/{x}/{y}");
        paths.add("/w/{x}/y");
        paths.add("/w/x/y");
        paths.add("/w/y/d");
        HttpServletRequestContentTypeMutator.orderKeysSoPathVariablesLast(paths);

        assertThat(
                paths,
                containsInAnyOrder("/w/x/y", "/w/x/{y}", "/w/y/d", "/w/{x}/y", "/w/{x}/{y}"));
    }
}
