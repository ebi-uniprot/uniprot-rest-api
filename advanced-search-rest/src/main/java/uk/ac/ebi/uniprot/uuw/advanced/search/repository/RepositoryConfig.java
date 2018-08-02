package uk.ac.ebi.uniprot.uuw.advanced.search.repository;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.solr.core.SolrTemplate;

/**
 *  Configure spring-data-solr repository beans, that are used to retrieve data from a solr instance.
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
    public HttpClient httpClient(RepositoryConfigProperties config) {
        // I am creating HttpClient exactly in the same way it is created inside CloudSolrClient.Builder,
        // but here I am just adding Credentials
        ModifiableSolrParams param = null;
        if(!config.getUsername().isEmpty() && !config.getPassword().isEmpty()){
            param = new ModifiableSolrParams();
            param.add(HttpClientUtil.PROP_BASIC_AUTH_USER,config.getUsername());
            param.add(HttpClientUtil.PROP_BASIC_AUTH_PASS,config.getPassword());
        }
        return HttpClientUtil.createClient(param);
    }

    @Bean
    public SolrClient solrClient(HttpClient httpClient,RepositoryConfigProperties config) {
        return new CloudSolrClient.Builder().withHttpClient(httpClient).withZkHost(config.getHost()).build();
    }

    @Bean
    public SolrTemplate solrTemplate(SolrClient solrClient) throws Exception {
        return new SolrTemplate(solrClient);
    }

}
