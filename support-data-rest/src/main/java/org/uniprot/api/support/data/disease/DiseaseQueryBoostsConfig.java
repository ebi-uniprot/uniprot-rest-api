package org.uniprot.api.support.data.disease;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.common.repository.search.QueryBoostsFileReader;

@Configuration
public class DiseaseQueryBoostsConfig {
    private static final String BOOSTS_RESOURCE_LOCATION = "/disease-query-boosts.config";

    @Bean
    public QueryBoosts diseaseQueryBoosts() {
        return new QueryBoostsFileReader(BOOSTS_RESOURCE_LOCATION).getQueryBoosts();
    }
}
