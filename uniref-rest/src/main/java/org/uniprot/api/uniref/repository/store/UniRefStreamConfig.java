package org.uniprot.api.uniref.repository.store;

import org.apache.http.client.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.uniprot.api.common.repository.store.StoreStreamer;
import org.uniprot.api.common.repository.store.TupleStreamTemplate;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.rest.respository.RepositoryConfigProperties;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.field.UniRefField;

/**
 * @author jluo
 * @date: 21 Aug 2019
 */
@Configuration
@Import(RepositoryConfig.class)
public class UniRefStreamConfig {

    @Bean
    public TupleStreamTemplate cloudSolrStreamTemplate(
            RepositoryConfigProperties configProperties, HttpClient httpClient) {
        return TupleStreamTemplate.builder()
                .collection(SolrCollection.uniref.name())
                .key(UniRefField.Search.id.name())
                .requestHandler("/export")
                .zookeeperHost(configProperties.getZkHost())
                .httpClient(httpClient)
                .build();
    }

    @Bean
    public StoreStreamer<UniRefEntry> unirefEntryStoreStreamer(
            UniRefStoreClient unirefClient, TupleStreamTemplate tupleStreamTemplate) {
        return StoreStreamer.<UniRefEntry>builder()
                .id(streamconfigProperties().getValueId())
                //   .defaultsField(streamconfigProperties().getDefaultsField())
                .streamerBatchSize(streamconfigProperties().getBatchSize())
                .storeClient(unirefClient)
                .tupleStreamTemplate(tupleStreamTemplate)
                .build();
    }

    @Bean
    public UniRefStreamConfigProperties streamconfigProperties() {
        return new UniRefStreamConfigProperties();
    }
}
