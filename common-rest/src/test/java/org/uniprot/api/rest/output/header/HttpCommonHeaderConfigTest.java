package org.uniprot.api.rest.output.header;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uniprot.api.rest.service.ServiceInfoConfig;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.mockito.Mockito.*;
import static org.springframework.http.HttpHeaders.*;
import static org.uniprot.api.rest.output.header.HttpCommonHeaderConfig.*;

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
                                .build());
    }

    @Test
    void cacheHeadersAdded() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getServletPath()).thenReturn("/uniprotkb/accession/P12345");
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);

        config.handleGatewayCaching(mockRequest, mockResponse);

        verify(mockResponse).addHeader(CACHE_CONTROL, PUBLIC_MAX_AGE + MAX_AGE);
        verify(mockResponse).addHeader(VARY, ACCEPT);
        verify(mockResponse).addHeader(VARY, ACCEPT_ENCODING);
        verify(mockResponse).addHeader(VARY, X_RELEASE_NUMBER);
    }

    @Test
    void noCacheHeadersAdded() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getServletPath()).thenReturn("/idmapping/run/12345");
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);

        config.handleGatewayCaching(mockRequest, mockResponse);

        verify(mockResponse).addHeader(CACHE_CONTROL, NO_CACHE);
        verify(mockResponse, never()).addHeader(CACHE_CONTROL, PUBLIC_MAX_AGE + MAX_AGE);
        verify(mockResponse).addHeader(VARY, ACCEPT);
        verify(mockResponse).addHeader(VARY, ACCEPT_ENCODING);
        verify(mockResponse).addHeader(VARY, X_RELEASE_NUMBER);
    }
}
