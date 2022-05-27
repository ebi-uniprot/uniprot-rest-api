package org.uniprot.api.rest.request;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.USER_AGENT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.FASTA_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.UNKNOWN_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.request.HttpServletRequestContentTypeMutator.COMPRESSED;
import static org.uniprot.api.rest.request.HttpServletRequestContentTypeMutator.FORMAT;

import java.util.*;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.ProducesRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.FileType;

/**
 * Created 10/12/19
 *
 * @author Edd
 */
class HttpServletRequestContentTypeMutatorTest {
    private HttpServletRequestContentTypeMutator requestContentTypeMutator;
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
        requestContentTypeMutator = new HttpServletRequestContentTypeMutator(handlerMapping);
    }

    @Test
    void requestWithAcceptedMediaTypeHeader_retainsMediaType() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        String host = "http://localhost";
        String uri = "/a/b/ENTITY";
        when(httpRequest.getRequestURL()).thenReturn(new StringBuffer(host + uri));
        when(httpRequest.getRequestURI()).thenReturn(uri);
        when(httpRequest.getServletPath()).thenReturn(uri);
        when(httpRequest.getContextPath()).thenReturn("");
        MediaType mediaType = UniProtMediaType.FF_MEDIA_TYPE;
        when(httpRequest.getHeader(ACCEPT)).thenReturn(mediaType.toString());
        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(httpRequest);

        requestContentTypeMutator.mutate(mutableRequest);
        assertThat(mutableRequest.getHeader(ACCEPT), is(mediaType.toString()));
        assertThat(mutableRequest.getRequestURL().toString(), is(host + uri));
        assertThat(mutableRequest.getRequestURI(), is(uri));
        assertThat(mutableRequest.getServletPath(), is(uri));
    }

    @Test
    void requestWithAcceptedExtension_setsMediaType() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        String entityPath = "/a/b/ENTITY";
        String uri = entityPath + ".fasta";
        String host = "http://localhost";
        when(httpRequest.getRequestURL()).thenReturn(new StringBuffer(host + uri));
        when(httpRequest.getRequestURI()).thenReturn(uri);
        when(httpRequest.getServletPath()).thenReturn(uri);
        when(httpRequest.getContextPath()).thenReturn("");
        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(httpRequest);

        requestContentTypeMutator.mutate(mutableRequest);

        assertThat(mutableRequest.getHeader(ACCEPT), is(FASTA_MEDIA_TYPE_VALUE));
        assertThat(mutableRequest.getRequestURL().toString(), is(host + entityPath));
        assertThat(mutableRequest.getRequestURI(), is(entityPath));
        assertThat(mutableRequest.getServletPath(), is(entityPath));
    }

    @Test
    void requestWithAcceptedFormatParameter_setsMediaType() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        String uri = "/a/b/ENTITY";
        when(httpRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost" + uri));
        when(httpRequest.getRequestURI()).thenReturn(uri);
        when(httpRequest.getServletPath()).thenReturn(uri);
        when(httpRequest.getContextPath()).thenReturn("");
        when(httpRequest.getParameter(FORMAT)).thenReturn("fasta");
        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(httpRequest);

        requestContentTypeMutator.mutate(mutableRequest);

        assertThat(mutableRequest.getHeader(ACCEPT), is(FASTA_MEDIA_TYPE_VALUE));
    }

    @Test
    void requestWithNotAcceptedFormatParameter_setsUnknownMediaTypeAndMessage() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        String uri = "/a/b/ENTITY";
        when(httpRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost" + uri));
        when(httpRequest.getRequestURI()).thenReturn(uri);
        when(httpRequest.getServletPath()).thenReturn(uri);
        when(httpRequest.getContextPath()).thenReturn("");
        when(httpRequest.getParameter(FORMAT)).thenReturn("obo");
        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(httpRequest);

        requestContentTypeMutator.mutate(mutableRequest);

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
        when(httpRequest.getServletPath()).thenReturn(uri);
        when(httpRequest.getContextPath()).thenReturn("");
        when(httpRequest.getParameter(FORMAT)).thenReturn("WRONG");
        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(httpRequest);

        requestContentTypeMutator.mutate(mutableRequest);

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
        when(httpRequest.getServletPath()).thenReturn(uri);
        when(httpRequest.getContextPath()).thenReturn("");
        when(httpRequest.getHeader(ACCEPT)).thenReturn(UniProtMediaType.OBO_MEDIA_TYPE.toString());
        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(httpRequest);

        requestContentTypeMutator.mutate(mutableRequest);

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
        when(httpRequest.getServletPath()).thenReturn(uri);
        when(httpRequest.getContextPath()).thenReturn("");
        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(httpRequest);

        requestContentTypeMutator.mutate(mutableRequest);

        assertThat(mutableRequest.getHeader(ACCEPT), is(APPLICATION_JSON_VALUE));
    }

    @Test
    void requestWithRandomAcceptHeaderAndIsBrowser_setsJsonMediaType() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        String uri = "/a/b/ENTITY";
        when(httpRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost" + uri));
        when(httpRequest.getRequestURI()).thenReturn(uri);
        when(httpRequest.getServletPath()).thenReturn(uri);
        when(httpRequest.getContextPath()).thenReturn("");
        when(httpRequest.getHeader(ACCEPT)).thenReturn("anything/at-all");
        when(httpRequest.getHeader(USER_AGENT))
                .thenReturn(
                        "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:75.0) Gecko/20100101 Firefox/75.0");

        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(httpRequest);

        requestContentTypeMutator.mutate(mutableRequest);

        assertThat(mutableRequest.getHeader(ACCEPT), is(APPLICATION_JSON_VALUE));
    }

    @Test
    void requestThatMatchesNoPath_isUntouched() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        String uri = "/a/b/c/d";

        when(httpRequest.getHeader(ACCEPT)).thenReturn(APPLICATION_JSON_VALUE);
        when(httpRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost" + uri));
        when(httpRequest.getRequestURI()).thenReturn(uri);
        when(httpRequest.getServletPath()).thenReturn(uri);
        when(httpRequest.getContextPath()).thenReturn("");
        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(httpRequest);

        requestContentTypeMutator.mutate(mutableRequest);

        assertThat(mutableRequest.getHeader(ACCEPT), is(APPLICATION_JSON_VALUE));
        verify(httpRequest, times(0)).setAttribute(anyString(), anyString());
    }

    @Test
    void correctPathIsFoundForRequest() {
        List<String> paths = requestContentTypeMutator.resourcePath2MediaTypesKeys;
        paths.add("/w/x/{id}");
        paths.add("/w/{id}/y");
        paths.add("/w/x/y");
        paths.add("/w/y/d");
        HttpServletRequestContentTypeMutator.orderKeysSoPathVariablesLast(paths);

        assertThat(requestContentTypeMutator.getMatchingPathPattern("/w/x/y"), is("/w/x/y"));

        assertThat(requestContentTypeMutator.getMatchingPathPattern("/w/x/y2"), is("/w/x/{id}"));

        assertThat(requestContentTypeMutator.getMatchingPathPattern("/w/x/y.txt"), is("/w/x/{id}"));

        assertThat(requestContentTypeMutator.getMatchingPathPattern("/X"), is(nullValue()));

        assertThat(requestContentTypeMutator.getMatchingPathPattern("/w/y/d"), is("/w/y/d"));

        assertThat(
                requestContentTypeMutator.getMatchingPathPattern("/w/anything.txt-thing;hello/y"),
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

    @Test
    void requestWithCompressedRequestTrue() {
        // given
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getHeader(ACCEPT)).thenReturn(APPLICATION_JSON_VALUE);
        when(httpRequest.getParameter(COMPRESSED)).thenReturn("true");
        String uri = "/a/b/ENTITY";
        when(httpRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost" + uri));
        when(httpRequest.getRequestURI()).thenReturn(uri);
        when(httpRequest.getServletPath()).thenReturn(uri);
        when(httpRequest.getContextPath()).thenReturn("");

        // when
        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(httpRequest);
        requestContentTypeMutator.mutate(mutableRequest);

        // then
        assertThat(mutableRequest.getHeader(HttpHeaders.ACCEPT_ENCODING), is("gzip"));
    }

    @Test
    void requestWithCompressedRequestTrueOverrideCurrentAcceptEncoding() {
        // given
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getHeader(ACCEPT)).thenReturn(APPLICATION_JSON_VALUE);
        when(httpRequest.getHeader(HttpHeaders.ACCEPT_ENCODING))
                .thenReturn(FileType.FILE.getFileType());
        when(httpRequest.getParameter(COMPRESSED)).thenReturn("true");
        String uri = "/a/b/ENTITY";
        when(httpRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost" + uri));
        when(httpRequest.getRequestURI()).thenReturn(uri);
        when(httpRequest.getServletPath()).thenReturn(uri);
        when(httpRequest.getContextPath()).thenReturn("");

        // when
        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(httpRequest);
        requestContentTypeMutator.mutate(mutableRequest);

        // then
        assertThat(
                mutableRequest.getHeader(HttpHeaders.ACCEPT_ENCODING),
                is(FileType.GZIP.getFileType()));
    }
}
