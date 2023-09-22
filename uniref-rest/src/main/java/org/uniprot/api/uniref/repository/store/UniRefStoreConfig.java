package org.uniprot.api.uniref.repository.store;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.uniprot.core.uniref.RepresentativeMember;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.store.datastore.voldemort.VoldemortClient;
import org.uniprot.store.datastore.voldemort.light.uniref.VoldemortRemoteUniRefEntryLightStore;
import org.uniprot.store.datastore.voldemort.member.uniref.VoldemortRemoteUniRefMemberStore;

/**
 * @author jluo
 * @date: 20 Aug 2019
 */
@Configuration
public class UniRefStoreConfig {
    @Bean
    public UniRefMemberStoreConfigProperties memberStoreConfigProperties() {
        return new UniRefMemberStoreConfigProperties();
    }

    @Bean
    public UniRefLightStoreConfigProperties lightStoreConfigProperties() {
        return new UniRefLightStoreConfigProperties();
    }

    @Bean
    @Profile("live")
    public UniRefMemberStoreClient uniRefStoreClient(
            UniRefMemberStoreConfigProperties unirefStoreConfigProperties) {
        VoldemortClient<RepresentativeMember> client =
                new VoldemortRemoteUniRefMemberStore(
                        unirefStoreConfigProperties.getNumberOfConnections(),
                        unirefStoreConfigProperties.isBrotliEnabled(),
                        unirefStoreConfigProperties.getStoreName(),
                        unirefStoreConfigProperties.getHost());
        return new UniRefMemberStoreClient(
                client, unirefStoreConfigProperties.getMemberBatchSize());
    }

    @Bean
    @Profile("live")
    public UniRefLightStoreClient uniRefLightStoreClient(
            UniRefLightStoreConfigProperties lightStoreConfigProperties) {
        VoldemortClient<UniRefEntryLight> client =
                new VoldemortRemoteUniRefEntryLightStore(
                        lightStoreConfigProperties.getNumberOfConnections(),
                        lightStoreConfigProperties.isBrotliEnabled(),
                        lightStoreConfigProperties.getStoreName(),
                        lightStoreConfigProperties.getHost());
        return new UniRefLightStoreClient(client);
    }
}
