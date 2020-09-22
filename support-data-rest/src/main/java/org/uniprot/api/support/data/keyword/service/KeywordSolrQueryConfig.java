package org.uniprot.api.support.data.keyword.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrQueryConfigFileReader;

@Configuration
public class KeywordSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/keyword-query.config";

    @Bean
    public SolrQueryConfig keywordSolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }
}
