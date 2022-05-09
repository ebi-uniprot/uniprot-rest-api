package org.uniprot.api.rest.respository;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;

@Slf4j
public class SolrPreemptiveAuthInterceptor implements HttpRequestInterceptor {

    private final RepositoryConfigProperties config;

    public SolrPreemptiveAuthInterceptor(RepositoryConfigProperties config) {
        this.config = config;
    }

    @Override
    public void process(HttpRequest request, HttpContext context)
            throws HttpException, IOException {
        AuthState authState = (AuthState) context.getAttribute(HttpClientContext.TARGET_AUTH_STATE);
        // If no auth scheme available yet, try to initialize it preemptively
        if (authState.getAuthScheme() == null) {
            log.info("No AuthState: set Basic Auth");

            HttpHost targetHost = (HttpHost) context.getAttribute(HttpCoreContext.HTTP_TARGET_HOST);
            AuthScope authScope = new AuthScope(targetHost.getHostName(), targetHost.getPort());

            CredentialsProvider credsProvider =
                    (CredentialsProvider) context.getAttribute(HttpClientContext.CREDS_PROVIDER);

            Credentials creds = credsProvider.getCredentials(authScope);
            if (creds == null) {
                log.info("No Basic Auth credentials: add them");
                creds = getCredentials(authScope);
            }
            authState.update(new BasicScheme(), creds);
        }
    }

    private Credentials getCredentials(AuthScope authScope) {
        if (!config.getUsername().isEmpty() && !config.getPassword().isEmpty()) {
            UsernamePasswordCredentials creds =
                    new UsernamePasswordCredentials(config.getUsername(), config.getPassword());

            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(authScope, creds);
            log.info("Creating Basic Auth credentials for user {}", config.getUsername());

            return credsProvider.getCredentials(authScope);
        } else {
            return null;
        }
    }
}
