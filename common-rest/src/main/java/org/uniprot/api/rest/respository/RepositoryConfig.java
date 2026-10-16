package org.uniprot.api.rest.respository;

import static java.util.Arrays.asList;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.core.util.Utils;

/**
 * Configure solr repository beans, that are used to retrieve data from a solr instance.
 *
 * @author lgonzales
 */
@Configuration
public class RepositoryConfig {

    @Bean
    public RepositoryConfigProperties repositoryConfigProperties() {
        return new RepositoryConfigProperties();
    }

    @Bean
    public UniProtKBRepositoryConfigProperties uniProtKBRepositoryConfigProperties() {
        return new UniProtKBRepositoryConfigProperties();
    }

    @Bean
    @Profile("live")
    public HttpClient httpClient(RepositoryConfigProperties config) {
        return buildHttpClient(config.getUsername(), config.getPassword());
    }

    @Bean
    @Profile("live")
    public SolrClient solrClient(HttpClient httpClient, RepositoryConfigProperties config) {
        return buildSolrClient(
                config.getZkHost(),
                config.getConnectionTimeout(),
                config.getSocketTimeout(),
                config.getHttphost(),
                config.getUsername(),
                config.getPassword());
    }

    @Bean
    @Profile("live")
    public SolrRequestConverter requestConverter() {
        return new SolrRequestConverter();
    }

    private HttpClient buildHttpClient(String username, String password) {
        // I am creating HttpClient exactly in the same way it is created inside
        // CloudSolrClient.Builder,
        // but here I am just adding Credentials
        ModifiableSolrParams params = new ModifiableSolrParams();
        if (Utils.notNullNotEmpty(username) && Utils.notNullNotEmpty(password)) {
            params = new ModifiableSolrParams();
            params.add(HttpClientUtil.PROP_BASIC_AUTH_USER, username);
            params.add(HttpClientUtil.PROP_BASIC_AUTH_PASS, password);
        }
        PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();

        return HttpClientUtil.createClient(params, manager, true);
    }

    public static SolrClient buildSolrClient(
            String zkHost,
            int connTimeout,
            int sockTimeout,
            String httpHost,
            String username,
            String password) {
        if (Utils.notNullNotEmpty(zkHost)) {
            String[] zookeeperHosts = zkHost.split(",");
            Http2SolrClient.Builder http2ClientBuilder =
                    new Http2SolrClient.Builder()
                            .withConnectionTimeout(connTimeout, TimeUnit.MILLISECONDS)
                            .withRequestTimeout(sockTimeout, TimeUnit.MILLISECONDS);
            if (Utils.notNullNotEmpty(username) && Utils.notNullNotEmpty(password)) {
                http2ClientBuilder.withBasicAuthCredentials(username, password);
            }
            return new CloudSolrClient.Builder(asList(zookeeperHosts), Optional.empty())
                    .withInternalClientBuilder(http2ClientBuilder)
                    .build();
        } else if (Utils.notNullNotEmpty(httpHost)) {
            return new HttpSolrClient.Builder().withBaseSolrUrl(httpHost).build();
        } else {
            throw new BeanCreationException(
                    "make sure your application.properties has right solr zookeeperhost or httphost properties");
        }
    }
}
