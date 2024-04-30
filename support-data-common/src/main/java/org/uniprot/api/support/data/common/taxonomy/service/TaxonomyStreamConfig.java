package org.uniprot.api.support.data.common.taxonomy.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.uniprot.api.common.repository.stream.document.DefaultDocumentIdStream;
import org.uniprot.api.rest.converter.SolrDocumentRdfIdConverter;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.support.data.common.taxonomy.repository.TaxonomyRepository;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;

import lombok.extern.slf4j.Slf4j;

/**
 * @author sahmad
 * @created 02/02/2021
 */
@Configuration
@Import(RepositoryConfig.class)
@Slf4j
public class TaxonomyStreamConfig {
    @Bean
    public DefaultDocumentIdStream<TaxonomyDocument> taxonomyDocumentIdStream(
            TaxonomyRepository repository) {
        return DefaultDocumentIdStream.<TaxonomyDocument>builder()
                .repository(repository)
                .documentToId(document -> new SolrDocumentRdfIdConverter().apply(document))
                .build();
    }
}
