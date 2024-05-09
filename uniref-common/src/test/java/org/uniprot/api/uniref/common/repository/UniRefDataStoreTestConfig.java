package org.uniprot.api.uniref.common.repository;

import static org.mockito.Mockito.mock;

import java.net.URISyntaxException;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.uniprot.api.uniref.common.repository.store.UniRefLightStoreClient;
import org.uniprot.api.uniref.common.repository.store.UniRefMemberStoreClient;
import org.uniprot.store.datastore.voldemort.light.uniref.VoldemortInMemoryUniRefEntryLightStore;
import org.uniprot.store.datastore.voldemort.member.uniref.VoldemortInMemoryUniRefMemberStore;

/**
 * @author jluo
 * @date: 23 Aug 2019
 */
@TestConfiguration
public class UniRefDataStoreTestConfig {

    @Bean
    @Profile("offline")
    public SolrClient unirefSolrClient() throws URISyntaxException {
        return mock(SolrClient.class);
    }

    @Bean
    @Profile("offline")
    public UniRefMemberStoreClient unirefMemberStoreClient() {
        return new UniRefMemberStoreClient(
                VoldemortInMemoryUniRefMemberStore.getInstance("uniref-member"), 5);
    }

    @Bean
    @Profile("offline")
    public UniRefLightStoreClient unirefLightStoreClient() {
        return new UniRefLightStoreClient(
                VoldemortInMemoryUniRefEntryLightStore.getInstance("uniref-light"));
    }
}
