package org.uniprot.api.support.data.common.keyword.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.uniprot.api.common.repository.stream.document.DefaultDocumentIdStream;
import org.uniprot.api.rest.converter.SolrDocumentRdfIdConverter;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.support.data.common.keyword.repository.KeywordRepository;
import org.uniprot.store.search.document.keyword.KeywordDocument;

import lombok.extern.slf4j.Slf4j;

/**
 * @author sahmad
 * @created 02/02/2021
 */
@Configuration
@Import(RepositoryConfig.class)
@Slf4j
public class KeywordStreamConfig {
    @Bean
    public DefaultDocumentIdStream<KeywordDocument> keywordDocumentIdStream(
            KeywordRepository repository) {
        return DefaultDocumentIdStream.<KeywordDocument>builder()
                .repository(repository)
                .documentToId(document -> new SolrDocumentRdfIdConverter().apply(document))
                .build();
    }
}
