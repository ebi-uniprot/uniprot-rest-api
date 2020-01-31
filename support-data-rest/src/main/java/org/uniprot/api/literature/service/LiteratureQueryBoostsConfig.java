package org.uniprot.api.literature.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.common.repository.search.QueryBoostsFileReader;

@Configuration
public class LiteratureQueryBoostsConfig {
    private static final String BOOSTS_RESOURCE_LOCATION = "/literature-query-boosts.config";

    @Bean
    public QueryBoosts literatureQueryBoosts() {
        return new QueryBoostsFileReader(BOOSTS_RESOURCE_LOCATION).getQueryBoosts();
    }
}
