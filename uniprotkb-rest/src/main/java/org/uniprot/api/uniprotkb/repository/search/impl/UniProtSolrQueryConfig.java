package org.uniprot.api.uniprotkb.repository.search.impl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrQueryConfigFileReader;

/**
 * Created 05/09/19
 *
 * @author Edd
 */
@Configuration
public class UniProtSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/uniprotkb-query.config";

    @Bean
    public SolrQueryConfig uniProtKBSolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }
}
