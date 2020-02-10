package org.uniprot.api.uniref.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.common.repository.search.QueryBoostsFileReader;

@Configuration
public class UniRefQueryBoostsConfig {
    private static final String BOOSTS_RESOURCE_LOCATION = "/uniref-query-boosts.config";

    @Bean
    public QueryBoosts uniRefQueryBoosts() {
        return new QueryBoostsFileReader(BOOSTS_RESOURCE_LOCATION).getQueryBoosts();
    }
}
