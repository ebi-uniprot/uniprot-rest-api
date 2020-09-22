package org.uniprot.api.uniref.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrQueryConfigFileReader;

@Configuration
public class UniRefSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/uniref-query.config";

    @Bean
    public SolrQueryConfig uniRefSolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }
}
