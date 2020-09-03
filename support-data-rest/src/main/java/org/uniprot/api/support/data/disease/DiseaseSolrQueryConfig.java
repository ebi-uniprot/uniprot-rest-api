package org.uniprot.api.support.data.disease;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrQueryConfigFileReader;

@Configuration
public class DiseaseSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/disease-query.config";

    @Bean
    public SolrQueryConfig diseaseSolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }
}
