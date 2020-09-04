package org.uniprot.api.uniparc.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrQueryConfigFileReader;

@Configuration
public class UniParcSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/uniparc-query.config";

    @Bean
    public SolrQueryConfig uniParcSolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }
}
