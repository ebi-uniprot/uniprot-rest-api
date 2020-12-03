package org.uniprot.api.uniref.repository.store;

import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.api.common.repository.store.RDFStreamerConfigProperties;
import org.uniprot.api.rest.output.RequestResponseLoggingInterceptor;
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
                        lightStoreConfigProperties.getStoreName(),
                        lightStoreConfigProperties.getHost());
        return new UniRefLightStoreClient(client);
    }

    @Bean(name = "rdfRestTemplate")
    @Profile("live")
    RestTemplate restTemplate(RDFStreamerConfigProperties rdfStreamerConfigProperties) {
        ClientHttpRequestFactory factory =
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(
                Collections.singletonList(new RequestResponseLoggingInterceptor()));
        restTemplate.setUriTemplateHandler(
                new DefaultUriBuilderFactory(rdfStreamerConfigProperties.getRequestUrl()));
        return restTemplate;
    }
}
