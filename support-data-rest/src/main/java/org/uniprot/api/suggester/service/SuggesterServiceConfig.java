package org.uniprot.api.suggester.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.solr.core.SolrTemplate;
import org.uniprot.store.search.SolrCollection;

/**
 * Provides beans used to give access to suggestions retrieved from a Solr instance.
 *
 * <p>Created 18/07/18
 *
 * @author Edd
 */
@Configuration
public class SuggesterServiceConfig {
    @Bean
    public SuggesterService suggesterService(SolrTemplate solrTemplate) {
        return new SuggesterService(solrTemplate, SolrCollection.suggest);
    }
}
