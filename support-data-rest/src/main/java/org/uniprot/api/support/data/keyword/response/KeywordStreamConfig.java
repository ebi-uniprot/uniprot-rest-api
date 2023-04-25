package org.uniprot.api.support.data.keyword.response;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.uniprot.api.common.repository.stream.document.DefaultDocumentIdStream;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.support.data.common.SolrDocumentRDFIdConverter;
import org.uniprot.api.support.data.keyword.repository.KeywordRepository;
import org.uniprot.store.search.document.keyword.KeywordDocument;

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
                .documentToId(document -> new SolrDocumentRDFIdConverter().apply(document))
                .build();
    }
}
