package org.uniprot.api.proteome.output;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.uniprot.api.common.repository.stream.document.DefaultDocumentIdStream;
import org.uniprot.api.proteome.repository.ProteomeQueryRepository;
import org.uniprot.api.rest.converter.SolrDocumentRdfIdConverter;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.store.search.document.proteome.ProteomeDocument;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Import(RepositoryConfig.class)
@Slf4j
public class ProteomeStreamConfig {

    @Bean
    public DefaultDocumentIdStream<ProteomeDocument> proteomeDocumentIdStream(
            ProteomeQueryRepository repository) {
        return DefaultDocumentIdStream.<ProteomeDocument>builder()
                .repository(repository)
                .documentToId(doc -> new SolrDocumentRdfIdConverter().apply(doc))
                .build();
    }
}
