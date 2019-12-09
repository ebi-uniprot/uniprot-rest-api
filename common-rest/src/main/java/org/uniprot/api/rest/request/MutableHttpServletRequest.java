package org.uniprot.api.rest.request;

import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.*;

/**
 * Created 03/12/2019
 *
 * @author Edd
 */
public class MutableHttpServletRequest extends HttpServletRequestWrapper {
    private final Map<String, String> customHeaders;
    private String uri;
    private StringBuffer url;

    public MutableHttpServletRequest(HttpServletRequest request) {
        super(request);
        this.customHeaders = new HashMap<>();
        this.uri = request.getRequestURI();
        this.url = request.getRequestURL();
    }

    public void setRequestURI(String uri) {
        this.uri = uri;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setAttribute(String x, Object o) {
        if (x.equals(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)) {
            Map<String, String> newUriVariables = (Map<String, String>) o;
            Map<String, String> uriVariables =
                    (Map<String, String>)
                            super.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            if (uriVariables == null) {
                super.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, o);
            } else {
                for (Map.Entry<String, String> newUriVariable : newUriVariables.entrySet()) {
                    uriVariables.putIfAbsent(newUriVariable.getKey(), newUriVariable.getValue());
                }
            }
        }
    }

    @Override
    public String getRequestURI() {
        return uri;
    }

    public void setRequestURL(String url) {
        this.url = new StringBuffer(url);
    }

    @Override
    public StringBuffer getRequestURL() {
        return url;
    }

    public void addHeader(String name, String value) {
        this.customHeaders.put(name, value);
    }

    @Override
    public String getHeader(String name) {
        // check the custom headers first
        String headerValue = customHeaders.get(name);

        if (!Objects.isNull(headerValue)) {
            return headerValue;
        }
        // else return from into the original wrapped object
        return ((HttpServletRequest) getRequest()).getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        // create a set of the custom header names
        Set<String> set = new HashSet<>(customHeaders.keySet());

        // now add the headers from the wrapped request object
        @SuppressWarnings("unchecked")
        Enumeration<String> e = ((HttpServletRequest) getRequest()).getHeaderNames();
        while (e.hasMoreElements()) {
            // add the names of the request headers into the list
            String n = e.nextElement();
            set.add(n);
        }

        // create an enumeration from the set and return
        return Collections.enumeration(set);
    }
}
