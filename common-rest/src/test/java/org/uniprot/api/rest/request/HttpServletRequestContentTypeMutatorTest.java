package org.uniprot.api.rest.request;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerMapping;
import org.uniprot.api.rest.output.UniProtMediaType;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.uniprot.api.rest.output.UniProtMediaType.DEFAULT_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.createUnknownMediaTypeForFileExtension;

/**
 * Created 10/12/19
 *
 * @author Edd
 */
class HttpServletRequestContentTypeMutatorTest {

    @Test
    void requestWithoutExtensionOrFormatParameter_isNotChanged() {
        String resource = "uniprot/another/resource";
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getRequestURI()).thenReturn(resource);
        when(mockRequest.getRequestURL())
                .thenReturn(new StringBuffer("http://www.uniprot.org/" + resource));
        when(mockRequest.getHeader(HttpHeaders.ACCEPT)).thenReturn("");

        MutableHttpServletRequest request = new MutableHttpServletRequest(mockRequest);

        HttpServletRequestContentTypeMutator.mutate(request);

        assertThat(request.getHeader(HttpHeaders.ACCEPT), is(DEFAULT_MEDIA_TYPE_VALUE));
    }

    @Test
    void searchRequestWithoutExtensionOrFormatParameter_isNotChanged() {
        String resource = "uniprot/another/search";
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getRequestURI()).thenReturn(resource);
        when(mockRequest.getRequestURL())
            .thenReturn(new StringBuffer("http://www.uniprot.org/" + resource));
        when(mockRequest.getHeader(HttpHeaders.ACCEPT)).thenReturn("");

        MutableHttpServletRequest request = new MutableHttpServletRequest(mockRequest);

        HttpServletRequestContentTypeMutator.mutate(request);

        assertThat(request.getHeader(HttpHeaders.ACCEPT), is(DEFAULT_MEDIA_TYPE_VALUE));
    }

    @Test
    void unknownFormat_causesException() {
        String resource = "/uniprot/api/uniprotkb/search";
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getRequestURI()).thenReturn(resource);
        when(mockRequest.getRequestURL())
                .thenReturn(new StringBuffer("http://www.uniprot.org/" + resource));
        when(mockRequest.getParameter("format")).thenReturn("XXXX");

        MutableHttpServletRequest request = new MutableHttpServletRequest(mockRequest);

        assertThat(request.getHeader(HttpHeaders.ACCEPT), is(nullValue()));

        HttpServletRequestContentTypeMutator.mutate(request);

        assertThat(
                request.getHeader(HttpHeaders.ACCEPT),
                is(createUnknownMediaTypeForFileExtension("XXXX").toString()));
    }

    @SuppressWarnings("unchecked")
    @Test
    void entryRequestUpdatesUriVariables() {
        MutableHttpServletRequest request =
                checkHeaders("txt", "/uniprot/api/entry-resource/P12345.txt");

        Object value = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        assertThat(value, is(notNullValue()));
        assertThat(value, instanceOf(Map.class));
        assertThat((Map<String, String>) value, hasEntry("entry-resource", "P12345"));
    }

    @ParameterizedTest(name = "Request: /search?format={0}&...")
    @MethodSource("provideMediaTypeStrings")
    void fileExtensionHandledCorrectlyForSearchRequest(String format) {
        checkHeaders(format, "/uniprot/api/uniprotkb/search");
    }

    @ParameterizedTest(name = "Request: /download?format={0}&...")
    @MethodSource("provideMediaTypeStrings")
    void fileExtensionHandledCorrectlyForDownloadRequest(String format) {
        checkHeaders(format, "/uniprot/api/uniprotkb/download");
    }

    @ParameterizedTest(name = "Request: /accession/P12345.{0}")
    @MethodSource("provideMediaTypeStrings")
    void fileExtensionHandledCorrectlyForEntryRequest(String format) {
        checkHeaders(format, "/uniprot/api/accession/P12345." + format);
    }

    private static Stream<Arguments> provideMediaTypeStrings() {
        return UniProtMediaType.ALL_TYPES.stream()
                .map(UniProtMediaType::getFileExtension)
                .map(Arguments::of);
    }

    private MutableHttpServletRequest checkHeaders(String format, String resource) {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
                .thenReturn(new HashMap<String, String>());
        when(mockRequest.getRequestURI()).thenReturn(resource);
        when(mockRequest.getRequestURL())
                .thenReturn(new StringBuffer("http://www.uniprot.org/" + resource));
        when(mockRequest.getParameter("format")).thenReturn(format);

        MutableHttpServletRequest request = new MutableHttpServletRequest(mockRequest);

        assertThat(request.getHeader(HttpHeaders.ACCEPT), is(nullValue()));

        HttpServletRequestContentTypeMutator.mutate(request);

        MediaType mediaTypeForFileExtension = UniProtMediaType.getMediaTypeForFileExtension(format);
        assertThat(request.getHeader(HttpHeaders.ACCEPT), is(mediaTypeForFileExtension.toString()));

        return request;
    }
}
