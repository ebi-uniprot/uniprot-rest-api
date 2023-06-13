package org.uniprot.api.uniprotkb.view.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.cv.ec.ECRepo;
import org.uniprot.cv.ec.ECRepoFactory;

@Configuration
public class ViewByServiceConfig {
    private final String ecDirectory;

    public ViewByServiceConfig(@Value("${solr.viewby.ecDir}") String ecDirectory) {
        this.ecDirectory = ecDirectory;
    }

    @Bean
    public ECRepo ecRepo() {
        return ECRepoFactory.get(ecDirectory);
    }
}
