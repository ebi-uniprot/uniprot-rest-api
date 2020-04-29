package org.uniprot.api.rest.request;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

import java.util.*;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Created 10/12/19
 *
 * @author Edd
 */
@Disabled
class MutableHttpServletRequestTest {

    @Test
    void canSetURI() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        MutableHttpServletRequest request = new MutableHttpServletRequest(mockRequest);
        String newResource = "my/other/resource";
        request.setRequestURI(newResource);
        assertThat(request.getRequestURI(), is(newResource));
    }

    @Test
    void canSetURL() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        MutableHttpServletRequest request = new MutableHttpServletRequest(mockRequest);
        String newResource = "my/other/resource/url";
        request.setRequestURL(newResource);
        assertThat(request.getRequestURL().toString(), is(newResource));
    }

    @Test
    void canAddHeader() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        MutableHttpServletRequest request = new MutableHttpServletRequest(mockRequest);
        String myHeader = "myHeader";
        String myHeaderValue = "myHeaderValue";
        request.addHeader(myHeader, myHeaderValue);
        assertThat(request.getHeader(myHeader), is(myHeaderValue));
    }

    @Test
    void canGetAllHeaderNames() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        List<String> originalHeaders = asList("header 1", "header 2");
        when(mockRequest.getHeaderNames()).thenReturn(Collections.enumeration(originalHeaders));

        MutableHttpServletRequest request = new MutableHttpServletRequest(mockRequest);
        String myHeader = "myHeader";
        request.addHeader(myHeader, "myHeaderValue");

        Enumeration<String> requestHeaderNamesEnum = request.getHeaderNames();

        Collection<String> requestHeaderNames = extractNamesFromEnumeration(requestHeaderNamesEnum);

        assertThat(requestHeaderNames, containsInAnyOrder("header 1", "header 2", myHeader));
    }

    private Collection<String> extractNamesFromEnumeration(Enumeration<String> enumeration) {
        Collection<String> toReturn = new ArrayList<>();

        while (enumeration.hasMoreElements()) {
            toReturn.add(enumeration.nextElement());
        }

        return toReturn;
    }

    @Test
    void canSetAttribute() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        MutableHttpServletRequest request = new MutableHttpServletRequest(mockRequest);
        request.setAttribute("anything", "anything");
        verify(mockRequest).setAttribute("anything", "anything");
    }

    @Test
    void canSetURITemplateVariablesAttribute() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);

        MutableHttpServletRequest request = new MutableHttpServletRequest(mockRequest);
        Map<String, String> myMap = new HashMap<>();
        myMap.put("hello", "world");
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, myMap);
        verify(mockRequest).setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, myMap);

        Map<String, String> mySecondMap = new HashMap<>();
        myMap.put("another", "one");

        when(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
                .thenReturn(myMap);
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, mySecondMap);

        assertThat(myMap, hasEntry("hello", "world"));
        assertThat(myMap, hasEntry("another", "one"));
    }
}
