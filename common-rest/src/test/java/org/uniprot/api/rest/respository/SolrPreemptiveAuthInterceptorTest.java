package org.uniprot.api.rest.respository;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AuthState;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SolrPreemptiveAuthInterceptorTest {

    @Test
    void canAddAuthentication() throws HttpException, IOException {
        RepositoryConfigProperties config = new RepositoryConfigProperties();
        config.setUsername("name");
        config.setPassword("password");
        SolrPreemptiveAuthInterceptor auth = new SolrPreemptiveAuthInterceptor(config);
        assertNotNull(auth);
        AuthState authState = Mockito.mock(AuthState.class);
        HttpContext context = Mockito.mock(HttpContext.class);
        Mockito.when(context.getAttribute(HttpClientContext.TARGET_AUTH_STATE))
                .thenReturn(authState);

        HttpHost httpHost = Mockito.mock(HttpHost.class);
        Mockito.when(httpHost.getHostName()).thenReturn("hostName");
        Mockito.when(httpHost.getPort()).thenReturn(8080);

        Mockito.when(context.getAttribute(HttpCoreContext.HTTP_TARGET_HOST)).thenReturn(httpHost);

        CredentialsProvider creds = Mockito.mock(CredentialsProvider.class);
        Mockito.when(context.getAttribute(HttpClientContext.CREDS_PROVIDER)).thenReturn(creds);
        HttpRequest request = Mockito.mock(HttpRequest.class);
        assertDoesNotThrow(() -> auth.process(request, context));
    }
}
