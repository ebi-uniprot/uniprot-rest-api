package org.uniprot.api.uniparc.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.common.repository.search.QueryBoostsFileReader;

@Configuration
public class UniParcQueryBoostsConfig {
    private static final String BOOSTS_RESOURCE_LOCATION = "/uniparc-query-boosts.config";

    @Bean
    public QueryBoosts uniParcQueryBoosts() {
        return new QueryBoostsFileReader(BOOSTS_RESOURCE_LOCATION).getQueryBoosts();
    }
}
