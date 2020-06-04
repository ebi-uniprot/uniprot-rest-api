package org.uniprot.api.support.data.keyword.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.common.repository.search.QueryBoostsFileReader;

@Configuration
public class KeywordQueryBoostsConfig {
    private static final String BOOSTS_RESOURCE_LOCATION = "/keyword-query-boosts.config";

    @Bean
    public QueryBoosts keywordQueryBoosts() {
        return new QueryBoostsFileReader(BOOSTS_RESOURCE_LOCATION).getQueryBoosts();
    }
}
