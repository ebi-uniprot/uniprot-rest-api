package org.uniprot.api.rest.output.header;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpHeaders.*;
import static org.uniprot.api.rest.output.header.HttpCommonHeaderConfig.*;
import static org.uniprot.api.rest.request.HttpServletRequestContentTypeMutator.COMPRESSED;
import static org.uniprot.api.rest.request.HttpServletRequestContentTypeMutator.FORMAT;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.uniprot.api.rest.output.context.FileType;
import org.uniprot.api.rest.request.MutableHttpServletRequest;
import org.uniprot.api.rest.service.ServiceInfoConfig;

class HttpCommonHeaderConfigTest {

    private static final String NON_CACHEABLE_PATHS_CSV = ".*/idmapping/.*";
    private HttpCommonHeaderConfig config;
    private static final Integer MAX_AGE = 40;

    @BeforeEach
    void setUp() {
        List<Pattern> nonCacheablePaths =
                Arrays.stream(NON_CACHEABLE_PATHS_CSV.split(","))
                        .map(Pattern::compile)
                        .collect(Collectors.toList());
        config =
                new HttpCommonHeaderConfig(
                        ServiceInfoConfig.ServiceInfo.builder()
                                .nonCacheablePaths(nonCacheablePaths)
                                .cacheControlMaxAge(MAX_AGE)
                                .build(),
                        mock(RequestMappingHandlerMapping.class));
    }

    @Test
    void cacheHeadersAddedForSuccessResponse() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getServletPath()).thenReturn("/uniprotkb/accession/P12345");
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        when(mockResponse.getStatus()).thenReturn(HttpServletResponse.SC_OK);

        config.handleGatewayCaching(mockRequest, mockResponse);

        verify(mockResponse).addHeader(CACHE_CONTROL, PUBLIC_MAX_AGE + MAX_AGE);
        verify(mockResponse).addHeader(VARY, ACCEPT);
        verify(mockResponse).addHeader(VARY, ACCEPT_ENCODING);
        verify(mockResponse).addHeader(VARY, X_UNIPROT_RELEASE);
        verify(mockResponse).addHeader(VARY, X_API_DEPLOYMENT_DATE);
    }

    @Test
    void cacheHeadersAddedForRedirectResponse() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getServletPath()).thenReturn("/uniprotkb/accession/P12345");
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        when(mockResponse.getStatus()).thenReturn(HttpServletResponse.SC_SEE_OTHER);

        config.handleGatewayCaching(mockRequest, mockResponse);

        verify(mockResponse).addHeader(CACHE_CONTROL, PUBLIC_MAX_AGE + MAX_AGE);
        verify(mockResponse).addHeader(VARY, ACCEPT);
        verify(mockResponse).addHeader(VARY, ACCEPT_ENCODING);
        verify(mockResponse).addHeader(VARY, X_UNIPROT_RELEASE);
        verify(mockResponse).addHeader(VARY, X_API_DEPLOYMENT_DATE);
    }

    @Test
    void noCacheHeadersAddedForBadRequestResponse() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getServletPath()).thenReturn("/uniprotkb/accession/P12345");
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        when(mockResponse.getStatus()).thenReturn(HttpServletResponse.SC_BAD_REQUEST);

        config.handleGatewayCaching(mockRequest, mockResponse);

        verify(mockResponse).addHeader(CACHE_CONTROL, NO_CACHE);
        verify(mockResponse, never()).addHeader(CACHE_CONTROL, PUBLIC_MAX_AGE + MAX_AGE);
        verify(mockResponse).addHeader(VARY, ACCEPT);
        verify(mockResponse).addHeader(VARY, ACCEPT_ENCODING);
        verify(mockResponse).addHeader(VARY, X_UNIPROT_RELEASE);
        verify(mockResponse).addHeader(VARY, X_API_DEPLOYMENT_DATE);
    }

    @Test
    void noCacheHeadersAddedForNonCacheablePaths() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getServletPath()).thenReturn("/idmapping/run/12345");
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        when(mockResponse.getStatus()).thenReturn(HttpServletResponse.SC_OK);

        config.handleGatewayCaching(mockRequest, mockResponse);

        verify(mockResponse).addHeader(CACHE_CONTROL, NO_CACHE);
        verify(mockResponse, never()).addHeader(CACHE_CONTROL, PUBLIC_MAX_AGE + MAX_AGE);
        verify(mockResponse).addHeader(VARY, ACCEPT);
        verify(mockResponse).addHeader(VARY, ACCEPT_ENCODING);
        verify(mockResponse).addHeader(VARY, X_UNIPROT_RELEASE);
        verify(mockResponse).addHeader(VARY, X_API_DEPLOYMENT_DATE);
    }

    @Test
    void downloadRequestDoesNotMutate() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        String entityPath = "/uniprotkb";
        String uri = entityPath + "/download/run";
        when(httpRequest.getRequestURI()).thenReturn(uri);
        when(httpRequest.getServletPath()).thenReturn(uri);
        when(httpRequest.getMethod()).thenReturn("POST");
        when(httpRequest.getParameter(FORMAT)).thenReturn("fasta");
        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(httpRequest);
        config.mutateRequestIfNeeded(mutableRequest);
        assertThat(mutableRequest.getHeader(ACCEPT), is(nullValue()));
        assertThat(mutableRequest.getRequestURI(), is(uri));
        assertThat(mutableRequest.getHeader(HttpHeaders.ACCEPT_ENCODING), is(nullValue()));
    }

    @Test
    void mutateAcceptEncodingHeaderForOtherPath() {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        String uri = "/a/b/ENTITY";
        when(httpRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost" + uri));
        when(httpRequest.getRequestURI()).thenReturn(uri);
        when(httpRequest.getServletPath()).thenReturn(uri);
        when(httpRequest.getContextPath()).thenReturn("");
        when(httpRequest.getParameter(COMPRESSED)).thenReturn("true");
        MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(httpRequest);
        config.mutateRequestIfNeeded(mutableRequest);
        assertThat(
                mutableRequest.getHeader(HttpHeaders.ACCEPT_ENCODING),
                is(FileType.GZIP.getFileType()));
    }
}
