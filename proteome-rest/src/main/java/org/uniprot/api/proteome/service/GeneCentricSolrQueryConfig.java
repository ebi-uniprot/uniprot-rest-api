package org.uniprot.api.proteome.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrQueryConfigFileReader;

@Configuration
public class GeneCentricSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/genecentric-query.config";

    @Bean
    public SolrQueryConfig geneCentricSolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }
}
