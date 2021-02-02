package org.uniprot.api.support.data.subcellular.response;

import java.util.Collections;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.api.common.repository.stream.document.DefaultDocumentIdStream;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamer;
import org.uniprot.api.common.repository.stream.rdf.RDFStreamerConfigProperties;
import org.uniprot.api.rest.output.RequestResponseLoggingInterceptor;
import org.uniprot.api.rest.respository.RepositoryConfig;
import org.uniprot.api.rest.service.RDFPrologs;
import org.uniprot.api.rest.service.RDFService;
import org.uniprot.api.support.data.common.RDFStreamConfig;
import org.uniprot.api.support.data.common.SolrDocumentRDFIdConverter;
import org.uniprot.api.support.data.subcellular.repository.SubcellularLocationRepository;
import org.uniprot.store.search.document.subcell.SubcellularLocationDocument;

/**
 * @author sahmad
 * @created 02/02/2021
 */
@Configuration
@Import(RepositoryConfig.class)
@Slf4j
public class SubcellularLocationStreamConfig {

    @Bean(name = "locationRDFStreamer")
    public RDFStreamer locationRDFStreamer(
            @Qualifier("locationRDFRestTemplate") RestTemplate restTemplate,
            DefaultDocumentIdStream<SubcellularLocationDocument> locationDocumentIdStream,
            RDFStreamerConfigProperties locationRDFConfigProperties) {

        RetryPolicy<Object> rdfRetryPolicy =
                RDFStreamConfig.rdfRetryPolicy(locationRDFConfigProperties);

        return RDFStreamer.builder()
                .rdfBatchSize(locationRDFConfigProperties.getBatchSize())
                .rdfFetchRetryPolicy(rdfRetryPolicy)
                .rdfService(new RDFService<>(restTemplate, String.class))
                .rdfProlog(RDFPrologs.SUBCELLULAR_LOCATION_PROLOG)
                .idStream(locationDocumentIdStream)
                .build();
    }

    @Bean
    @ConfigurationProperties(prefix = "location.streamer.rdf")
    public RDFStreamerConfigProperties locationRDFConfigProperties() {
        return new RDFStreamerConfigProperties();
    }

    @Bean
    public DefaultDocumentIdStream<SubcellularLocationDocument> locationDocumentIdStream(
            SubcellularLocationRepository repository) {
        return DefaultDocumentIdStream.<SubcellularLocationDocument>builder()
                .repository(repository)
                .documentToId(document -> new SolrDocumentRDFIdConverter().apply(document))
                .build();
    }

    @Bean(name = "locationRDFRestTemplate")
    @Profile("live")
    RestTemplate locationRDFRestTemplate(RDFStreamerConfigProperties locationRDFConfigProperties) {
        ClientHttpRequestFactory factory =
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(
                Collections.singletonList(new RequestResponseLoggingInterceptor()));
        restTemplate.setUriTemplateHandler(
                new DefaultUriBuilderFactory(locationRDFConfigProperties.getRequestUrl()));
        return restTemplate;
    }
}
