package org.uniprot.api.support.data.suggester.service;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    public SuggesterService suggesterService(@Qualifier("nonKBSolrClient") SolrClient solrClient) {
        return new SuggesterService(solrClient, SolrCollection.suggest);
    }
}
