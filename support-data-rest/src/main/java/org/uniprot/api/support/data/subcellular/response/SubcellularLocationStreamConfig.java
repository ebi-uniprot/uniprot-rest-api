package org.uniprot.api.support.data.subcellular.response;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.uniprot.api.common.repository.stream.document.DefaultDocumentIdStream;
import org.uniprot.api.rest.converter.SolrDocumentRdfIdConverter;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.support.data.subcellular.repository.SubcellularLocationRepository;
import org.uniprot.store.search.document.subcell.SubcellularLocationDocument;

import lombok.extern.slf4j.Slf4j;

/**
 * @author sahmad
 * @created 02/02/2021
 */
@Configuration
@Import(RepositoryConfig.class)
@Slf4j
public class SubcellularLocationStreamConfig {
    @Bean
    public DefaultDocumentIdStream<SubcellularLocationDocument> locationDocumentIdStream(
            SubcellularLocationRepository repository) {
        return DefaultDocumentIdStream.<SubcellularLocationDocument>builder()
                .repository(repository)
                .documentToId(document -> new SolrDocumentRdfIdConverter().apply(document))
                .build();
    }
}
