package org.uniprot.api.support.data.literature.response;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.uniprot.api.common.repository.stream.document.DefaultDocumentIdStream;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.support.data.common.SolrDocumentRDFIdConverter;
import org.uniprot.api.support.data.literature.repository.LiteratureRepository;
import org.uniprot.store.search.document.literature.LiteratureDocument;

/**
 * @author sahmad
 * @created 02/02/2021
 */
@Configuration
@Import(RepositoryConfig.class)
@Slf4j
public class LiteratureStreamConfig {
    @Bean
    public DefaultDocumentIdStream<LiteratureDocument> literatureDocumentIdStream(
            LiteratureRepository repository) {
        return DefaultDocumentIdStream.<LiteratureDocument>builder()
                .repository(repository)
                .documentToId(document -> new SolrDocumentRDFIdConverter().apply(document))
                .build();
    }
}
