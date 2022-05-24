package org.uniprot.api.rest.respository;

import static java.util.Arrays.asList;

import java.util.Objects;
import java.util.Optional;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
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
                httpClient,
                config.getZkHost(),
                config.getConnectionTimeout(),
                config.getSocketTimeout(),
                config.getHttphost());
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
        ModifiableSolrParams params = null;
        if (Utils.notNullNotEmpty(username) && Utils.notNullNotEmpty(password)) {
            params = new ModifiableSolrParams();
            params.add(HttpClientUtil.PROP_BASIC_AUTH_USER, username);
            params.add(HttpClientUtil.PROP_BASIC_AUTH_PASS, password);
        }
        PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();

        if (Objects.isNull(params)) {
            params = new ModifiableSolrParams();
        }
        return HttpClientUtil.createClient(params, manager, true);
    }

    private SolrClient buildSolrClient(
            HttpClient httpClient,
            String zkHost,
            int connTimeout,
            int sockTimeout,
            String httpHost) {
        if (Utils.notNullNotEmpty(zkHost)) {
            String[] zookeeperHosts = zkHost.split(",");
            return new CloudSolrClient.Builder(asList(zookeeperHosts), Optional.empty())
                    .withConnectionTimeout(connTimeout)
                    .withHttpClient(httpClient)
                    .withSocketTimeout(sockTimeout)
                    .build();
        } else if (Utils.notNullNotEmpty(httpHost)) {
            return new HttpSolrClient.Builder().withBaseSolrUrl(httpHost).build();
        } else {
            throw new BeanCreationException(
                    "make sure your application.properties has right solr zookeeperhost or httphost properties");
        }
    }
}
