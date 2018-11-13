package uk.ac.ebi.uniprot.uuw.suggester.service;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides beans used to give access to suggestions retrieved from a Solr instance.
 *
 * Created 18/07/18
 *
 * @author Edd
 */
@Configuration
public class SuggesterServiceConfig {
    @Bean
    public SolrConfigProperties solrConfigProperties() {
        return new SolrConfigProperties();
    }

    @Bean
    public SuggesterService suggesterService(SolrClient solrClient, SolrConfigProperties solrConfigProperties) {
        return new SuggesterService(solrClient, solrConfigProperties.getCollectionName());
    }
}
