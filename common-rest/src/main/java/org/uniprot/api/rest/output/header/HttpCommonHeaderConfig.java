package org.uniprot.api.rest.output.header;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;
import org.uniprot.api.rest.request.HttpServletRequestContentTypeMutator;
import org.uniprot.api.rest.request.MutableHttpServletRequest;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS;

/**
 * Defines common HTTP headers which can be imported to any REST module.
 *
 * <p>Created 05/09/18
 *
 * @author Edd
 */
@Configuration
public class HttpCommonHeaderConfig {
    static final String ALLOW_ALL_ORIGINS = "*";

    /**
     * Defines a simple request filter that adds an Access-Control-Allow-Origin header with the
     * value '*' to REST requests.
     *
     * <p>The reason for explicitly providing an all origins value, '*', is that web-caching of
     * requests from one origin interferes with those from another origin, even when the same
     * resource is fetched.
     *
     * @return
     */
    @Bean
    public OncePerRequestFilter originsFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                    throws ServletException, IOException {
                MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(request);
                HttpServletRequestContentTypeMutator.mutate(mutableRequest);

                response.addHeader(ACCESS_CONTROL_ALLOW_ORIGIN, ALLOW_ALL_ORIGINS);
                response.addHeader(ACCESS_CONTROL_EXPOSE_HEADERS, "Link, X-TotalRecords");

                chain.doFilter(mutableRequest, response);
            }
        };
    }
}
