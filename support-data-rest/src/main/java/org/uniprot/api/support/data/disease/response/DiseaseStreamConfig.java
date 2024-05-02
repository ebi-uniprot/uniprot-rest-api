package org.uniprot.api.support.data.disease.response;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.uniprot.api.common.repository.stream.document.DefaultDocumentIdStream;
import org.uniprot.api.rest.converter.SolrDocumentRdfIdConverter;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.support.data.disease.repository.DiseaseRepository;
import org.uniprot.store.search.document.disease.DiseaseDocument;

import lombok.extern.slf4j.Slf4j;

/**
 * @author sahmad
 * @created 02/02/2021
 */
@Configuration
@Import(RepositoryConfig.class)
@Slf4j
public class DiseaseStreamConfig {

    @Bean
    public DefaultDocumentIdStream<DiseaseDocument> diseaseDocumentIdStream(
            DiseaseRepository repository) {
        return DefaultDocumentIdStream.<DiseaseDocument>builder()
                .repository(repository)
                .documentToId(doc -> new SolrDocumentRdfIdConverter().apply(doc))
                .build();
    }
}
