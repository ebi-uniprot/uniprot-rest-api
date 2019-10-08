package org.uniprot.api.uniprotkb.repository.store;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.uniprot.api.common.repository.store.StoreStreamer;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotQueryRepository;
import org.uniprot.core.uniprot.UniProtEntry;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

/**
 * Created 21/08/18
 *
 * @author Edd
 */
@Configuration
@Import(RepositoryConfig.class)
public class ResultsConfig {
    @Bean
    public StoreStreamer<UniProtDocument, UniProtEntry> uniProtEntryStoreStreamer(
            UniProtKBStoreClient uniProtClient, UniprotQueryRepository uniprotQueryRepository) {
        return StoreStreamer.<UniProtDocument, UniProtEntry>builder()
                .storeBatchSize(resultsConfigProperties().getUniprot().getStoreBatchSize())
                .searchBatchSize(resultsConfigProperties().getUniprot().getSearchBatchSize())
                .storeClient(uniProtClient)
                .documentToId(doc -> doc.accession)
                .repository(uniprotQueryRepository)
                .build();
    }

    @Bean
    public StreamerConfigProperties resultsConfigProperties() {
        return new StreamerConfigProperties();
    }
}
