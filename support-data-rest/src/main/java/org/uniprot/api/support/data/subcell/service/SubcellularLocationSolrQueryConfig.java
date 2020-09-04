package org.uniprot.api.support.data.subcell.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrQueryConfigFileReader;

@Configuration
public class SubcellularLocationSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/subcell-query.config";

    @Bean
    public SolrQueryConfig subcellSolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }
}
