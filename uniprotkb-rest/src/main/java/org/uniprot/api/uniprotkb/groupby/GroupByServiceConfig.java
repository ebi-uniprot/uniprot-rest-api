package org.uniprot.api.uniprotkb.groupby;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.cv.ec.ECRepo;
import org.uniprot.cv.ec.ECRepoFactory;

@Configuration
public class GroupByServiceConfig {
    private final String ecDirectory;

    public GroupByServiceConfig(@Value("${groupby.solr.ec.dir}") String ecDirectory) {
        this.ecDirectory = ecDirectory;
    }

    @Bean
    public ECRepo ecRepo() {
        return ECRepoFactory.get(ecDirectory);
    }
}
