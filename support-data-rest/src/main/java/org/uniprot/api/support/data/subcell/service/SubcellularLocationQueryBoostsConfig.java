package org.uniprot.api.support.data.subcell.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.common.repository.search.QueryBoostsFileReader;

@Configuration
public class SubcellularLocationQueryBoostsConfig {
    private static final String BOOSTS_RESOURCE_LOCATION = "/subcell-query-boosts.config";

    @Bean
    public QueryBoosts subcellQueryBoosts() {
        return new QueryBoostsFileReader(BOOSTS_RESOURCE_LOCATION).getQueryBoosts();
    }
}
