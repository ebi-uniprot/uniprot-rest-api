package org.uniprot.api.support.data.crossref.response;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.uniprot.api.common.repository.stream.document.DefaultDocumentIdStream;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.support.data.common.SolrDocumentRDFIdConverter;
import org.uniprot.api.support.data.crossref.repository.CrossRefRepository;
import org.uniprot.store.search.document.dbxref.CrossRefDocument;

/**
 * @author sahmad
 * @created 01/02/2021
 */
@Configuration
@Import(RepositoryConfig.class)
@Slf4j
public class CrossRefStreamConfig {

    @Bean
    public DefaultDocumentIdStream<CrossRefDocument> xrefDocumentIdStream(
            CrossRefRepository repository) {
        return DefaultDocumentIdStream.<CrossRefDocument>builder()
                .repository(repository)
                .documentToId(document -> new SolrDocumentRDFIdConverter().apply(document))
                .build();
    }
}
