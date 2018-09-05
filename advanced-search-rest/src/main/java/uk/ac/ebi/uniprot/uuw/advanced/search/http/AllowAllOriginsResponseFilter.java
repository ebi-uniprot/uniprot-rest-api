package uk.ac.ebi.uniprot.uuw.advanced.search.http;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;

/**
 * Defines a simple request filter that adds an Access-Control-Allow-Origin header with the value '*' to REST
 * requests.
 *
 * The reason for explicitly providing an all origins value, '*', is that web-caching of requests from one
 * origin interferes with those from another origin, even when the same resource is fetched.
 *
 * Created 05/09/18
 * @author Edd
 */
@Component
public class AllowAllOriginsResponseFilter extends OncePerRequestFilter {
    static final String ALLOW_ALL_ORIGINS = "*";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        response.addHeader(ACCESS_CONTROL_ALLOW_ORIGIN, ALLOW_ALL_ORIGINS);
        filterChain.doFilter(request, response);
    }
}
