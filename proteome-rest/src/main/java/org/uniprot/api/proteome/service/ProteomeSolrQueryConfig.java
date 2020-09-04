package org.uniprot.api.proteome.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrQueryConfigFileReader;

@Configuration
public class ProteomeSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/proteome-query.config";

    @Bean
    public SolrQueryConfig proteomeSolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }
}
