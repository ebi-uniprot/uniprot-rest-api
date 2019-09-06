package org.uniprot.api.uniprotkb.repository.search.impl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.common.repository.search.QueryBoostsFileReader;

/**
 * Created 05/09/19
 *
 * @author Edd
 */
@Configuration
public class UniProtQueryBoostsConfig {
    private static final String BOOSTS_RESOURCE_LOCATION = "uniprotkb-query-boosts.config";

    @Bean
    public QueryBoosts uniProtKBQueryBoosts() {
        return new QueryBoostsFileReader(BOOSTS_RESOURCE_LOCATION).getQueryBoosts();
    }
}
