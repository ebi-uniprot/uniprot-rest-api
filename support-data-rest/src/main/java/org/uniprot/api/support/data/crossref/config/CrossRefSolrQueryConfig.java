package org.uniprot.api.support.data.crossref.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrQueryConfigFileReader;

@Configuration
public class CrossRefSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/crossref-query.config";

    @Bean
    public SolrQueryConfig crossRefSolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }
}
