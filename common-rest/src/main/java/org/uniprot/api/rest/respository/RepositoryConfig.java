package org.uniprot.api.rest.respository;

import dev.failsafe.RateLimiter;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.uniprot.api.common.concurrency.RateLimits;
import org.uniprot.api.common.repository.search.SolrRequestConverter;

import java.time.Duration;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.uniprot.api.common.concurrency.RateLimits.GET_ALL_RATE_LIMIT_PER_MINUTE;
import static org.uniprot.api.common.concurrency.RateLimits.SEARCH_RATE_LIMIT_PER_MINUTE;

/**
 * Configure solr repository beans, that are used to retrieve data from a solr instance.
 *
 * @author lgonzales
 */
@Configuration
public class RepositoryConfig {
    @Value("${rateLimitPerSecond.search:" + SEARCH_RATE_LIMIT_PER_MINUTE + "}")
    private int searchRateLimitPerSecond;

    @Value("${rateLimitPerSecond.getAll:" + GET_ALL_RATE_LIMIT_PER_MINUTE + "}")
    private int getAllRateLimitPerSecond;

    @Bean
    public RateLimits rateLimits() {
        return RateLimits.builder()
                .searchRateLimiter(
                        RateLimiter.smoothBuilder(searchRateLimitPerSecond, Duration.ofSeconds(1))
                                .build())
                .getAllRateLimiter(
                        RateLimiter.smoothBuilder(getAllRateLimitPerSecond, Duration.ofSeconds(1))
                                .build())
                .build();
    }

    @Bean
    public RepositoryConfigProperties repositoryConfigProperties() {
        return new RepositoryConfigProperties();
    }

    @Bean
    @Profile("live")
    public HttpClient httpClient(RepositoryConfigProperties config) {
        // I am creating HttpClient exactly in the same way it is created inside
        // CloudSolrClient.Builder,
        // but here I am just adding Credentials
        ModifiableSolrParams param = null;
        if (!config.getUsername().isEmpty() && !config.getPassword().isEmpty()) {
            param = new ModifiableSolrParams();
            param.add(HttpClientUtil.PROP_BASIC_AUTH_USER, config.getUsername());
            param.add(HttpClientUtil.PROP_BASIC_AUTH_PASS, config.getPassword());
        }

        PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();

        return HttpClientUtil.createClient(param, manager, true);
    }

    @Bean
    @Profile("live")
    public SolrClient solrClient(HttpClient httpClient, RepositoryConfigProperties config) {
        String zookeeperhost = config.getZkHost();
        if (zookeeperhost != null && !zookeeperhost.isEmpty()) {
            String[] zookeeperHosts = zookeeperhost.split(",");
            return new CloudSolrClient.Builder(asList(zookeeperHosts), Optional.empty())
                    .withConnectionTimeout(config.getConnectionTimeout())
                    .withHttpClient(httpClient)
                    .withSocketTimeout(config.getSocketTimeout())
                    .build();
        } else if (!config.getHttphost().isEmpty()) {
            return new HttpSolrClient.Builder().withBaseSolrUrl(config.getHttphost()).build();
        } else {
            throw new BeanCreationException(
                    "make sure your application.properties has eight solr zookeeperhost or httphost properties");
        }
    }

    @Bean
    @Profile("live")
    public SolrRequestConverter requestConverter() {
        return new SolrRequestConverter();
    }
}
