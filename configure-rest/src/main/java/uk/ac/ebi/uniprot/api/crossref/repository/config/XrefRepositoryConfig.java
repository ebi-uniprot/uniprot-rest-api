package uk.ac.ebi.uniprot.api.crossref.repository.config;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.solr.core.SolrTemplate;

import java.util.Arrays;
import java.util.Optional;

@Configuration
public class XrefRepositoryConfig {
    @Value("${spring.data.solr.zkHost}")
    private String zkHostString;

    @Bean
    public SolrClient solrClient() {
        SolrClient solrClient = new CloudSolrClient.Builder(Arrays.asList(zkHostString), Optional.empty()).build();
        return solrClient;

    }

    @Bean
    public SolrTemplate solrTemplate(SolrClient solrClient) {
        return new SolrTemplate(solrClient);
    }
}
