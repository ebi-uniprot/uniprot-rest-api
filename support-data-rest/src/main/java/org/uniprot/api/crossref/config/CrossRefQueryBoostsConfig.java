package org.uniprot.api.crossref.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.common.repository.search.QueryBoostsFileReader;

@Configuration
public class CrossRefQueryBoostsConfig {
    private static final String BOOSTS_RESOURCE_LOCATION = "/crossref-query-boosts.config";

    @Bean
    public QueryBoosts crossRefQueryBoosts() {
        return new QueryBoostsFileReader(BOOSTS_RESOURCE_LOCATION).getQueryBoosts();
    }
}
