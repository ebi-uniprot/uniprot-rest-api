package org.uniprot.api.support.data.disease.response;

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
import org.uniprot.api.support.data.disease.repository.DiseaseRepository;
import org.uniprot.store.search.document.disease.DiseaseDocument;

/**
 * @author sahmad
 * @created 02/02/2021
 */
@Configuration
@Import(RepositoryConfig.class)
@Slf4j
public class DiseaseStreamConfig {

    @Bean
    public RDFStreamer diseaseRDFStreamer(
            @Qualifier("diseaseRDFRestTemplate") RestTemplate restTemplate,
            DefaultDocumentIdStream<DiseaseDocument> diseaseDocumentIdStream,
            RDFStreamerConfigProperties diseaseRDFConfigProperties) {

        RetryPolicy<Object> rdfRetryPolicy =
                RDFStreamConfig.rdfRetryPolicy(diseaseRDFConfigProperties);

        return RDFStreamer.builder()
                .rdfBatchSize(diseaseRDFConfigProperties.getBatchSize())
                .rdfFetchRetryPolicy(rdfRetryPolicy)
                .rdfService(new RDFService<>(restTemplate, String.class))
                .rdfProlog(RDFPrologs.DISEASE_PROLOG)
                .idStream(diseaseDocumentIdStream)
                .build();
    }

    @Bean
    @ConfigurationProperties(prefix = "disease.streamer.rdf")
    public RDFStreamerConfigProperties diseaseRDFConfigProperties() {
        return new RDFStreamerConfigProperties();
    }

    @Bean
    public DefaultDocumentIdStream<DiseaseDocument> diseaseDocumentIdStream(
            DiseaseRepository repository) {
        return DefaultDocumentIdStream.<DiseaseDocument>builder()
                .repository(repository)
                .documentToId(doc -> new SolrDocumentRDFIdConverter().apply(doc))
                .build();
    }

    @Bean(name = "diseaseRDFRestTemplate")
    @Profile("live")
    RestTemplate diseaseRestTemplate(RDFStreamerConfigProperties diseaseRDFConfigProperties) {
        ClientHttpRequestFactory factory =
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(
                Collections.singletonList(new RequestResponseLoggingInterceptor()));
        restTemplate.setUriTemplateHandler(
                new DefaultUriBuilderFactory(diseaseRDFConfigProperties.getRequestUrl()));
        return restTemplate;
    }
}
