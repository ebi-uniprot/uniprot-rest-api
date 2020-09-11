package org.uniprot.api.support.data.taxonomy.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrQueryConfigFileReader;

@Configuration
public class TaxonomySolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/taxonomy-query.config";

    @Bean
    public SolrQueryConfig taxonomySolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }
}
