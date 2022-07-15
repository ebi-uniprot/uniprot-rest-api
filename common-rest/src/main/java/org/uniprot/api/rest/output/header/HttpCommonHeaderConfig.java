package org.uniprot.api.rest.output.header;

import static org.springframework.http.HttpHeaders.*;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.uniprot.api.rest.request.HttpServletRequestContentTypeMutator;
import org.uniprot.api.rest.request.MutableHttpServletRequest;
import org.uniprot.api.rest.service.ServiceInfoConfig;
import org.uniprot.core.util.Utils;

/**
 * Defines common HTTP headers which can be imported to any REST module.
 *
 * <p>Created 05/09/18
 *
 * @author Edd
 */
@Configuration
@Import({ServiceInfoConfig.class})
public class HttpCommonHeaderConfig {
    public static final String X_UNIPROT_RELEASE = "X-UniProt-Release";
    public static final String X_UNIPROT_RELEASE_DATE = "X-UniProt-Release-Date";
    public static final String X_API_DEPLOYMENT_DATE = "X-API-Deployment-Date";
    static final String ALLOW_ALL_ORIGINS = "*";
    public static final String X_TOTAL_RESULTS = "X-Total-Results";

    static final String PUBLIC_MAX_AGE = "public, max-age=";
    static final String NO_CACHE = "no-cache";
    private final ServiceInfoConfig.ServiceInfo serviceInfo;
    private final HttpServletRequestContentTypeMutator requestContentTypeMutator;

    @Autowired
    public HttpCommonHeaderConfig(
            ServiceInfoConfig.ServiceInfo serviceInfo,
            RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this.serviceInfo = serviceInfo;
        this.requestContentTypeMutator =
                new HttpServletRequestContentTypeMutator(requestMappingHandlerMapping);
    }

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
    @Bean("oncePerRequestFilter")
    public OncePerRequestFilter oncePerRequestFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                    throws ServletException, IOException {
                MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(request);
                requestContentTypeMutator.mutate(mutableRequest);

                response.addHeader(ACCESS_CONTROL_ALLOW_ORIGIN, ALLOW_ALL_ORIGINS);
                response.addHeader(
                        ACCESS_CONTROL_EXPOSE_HEADERS,
                        "Link, "
                                + X_TOTAL_RESULTS
                                + ", "
                                + X_UNIPROT_RELEASE
                                + ", "
                                + X_UNIPROT_RELEASE_DATE
                                + ", "
                                + X_API_DEPLOYMENT_DATE);
                response.addHeader(X_UNIPROT_RELEASE, serviceInfo.getReleaseNumber());
                response.addHeader(X_UNIPROT_RELEASE_DATE, serviceInfo.getReleaseDate());
                response.addHeader(X_API_DEPLOYMENT_DATE, serviceInfo.getDeploymentDate());
                handleGatewayCaching(request, response);
                chain.doFilter(mutableRequest, response);
            }
        };
    }

    /**
     * Ensure gate-way caching uses accept/accept-encoding headers as a key
     *
     * @param request the request
     * @param response the response to modify
     */
    void handleGatewayCaching(HttpServletRequest request, HttpServletResponse response) {
        boolean requiresCachingHeaders = true;
        for (Pattern pattern : serviceInfo.getNonCacheablePaths()) {
            if (Utils.notNull(request)
                    && Utils.notNullNotEmpty(request.getServletPath())
                    && pattern.matcher(request.getServletPath()).matches()) {
                requiresCachingHeaders = false;
                break;
            }
        }
        if (requiresCachingHeaders) {
            // request gateway caching
            response.addHeader(CACHE_CONTROL, PUBLIC_MAX_AGE + serviceInfo.getMaxAgeInSeconds());
        } else {
            // explictly assert to any gate-way cache that we do not want caching
            response.addHeader(CACHE_CONTROL, NO_CACHE);
        }

        // used so that any gate-way caching that takes place uses accept/accept-encoding headers as
        // a key
        response.addHeader(VARY, ACCEPT);
        response.addHeader(VARY, ACCEPT_ENCODING);
        response.addHeader(VARY, X_UNIPROT_RELEASE);
        response.addHeader(VARY, X_API_DEPLOYMENT_DATE);
    }
}
