package org.uniprot.api.support.data.crossref.response;

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

    @Bean(name = "xrefRDFStreamer")
    public RDFStreamer xrefRDFStreamer(
            @Qualifier("xrefRDFRestTemplate") RestTemplate restTemplate,
            DefaultDocumentIdStream<CrossRefDocument> xrefDocumentIdStream,
            RDFStreamerConfigProperties xrefRDFConfigProperties) {

        RetryPolicy<Object> rdfRetryPolicy =
                RDFStreamConfig.rdfRetryPolicy(xrefRDFConfigProperties);

        return RDFStreamer.builder()
                .rdfBatchSize(xrefRDFConfigProperties.getBatchSize())
                .rdfFetchRetryPolicy(rdfRetryPolicy)
                .rdfService(new RDFService<>(restTemplate, String.class))
                .rdfProlog(RDFPrologs.XREF_PROLOG)
                .idStream(xrefDocumentIdStream)
                .build();
    }

    @Bean
    @ConfigurationProperties(prefix = "xref.streamer.rdf")
    public RDFStreamerConfigProperties xrefRDFConfigProperties() {
        return new RDFStreamerConfigProperties();
    }

    @Bean
    public DefaultDocumentIdStream<CrossRefDocument> xrefDocumentIdStream(
            CrossRefRepository repository) {
        return DefaultDocumentIdStream.<CrossRefDocument>builder()
                .repository(repository)
                .documentToId(document -> new SolrDocumentRDFIdConverter().apply(document))
                .build();
    }

    @Bean(name = "xrefRDFRestTemplate")
    @Profile("live")
    RestTemplate restTemplate(RDFStreamerConfigProperties xrefRDFConfigProperties) {
        ClientHttpRequestFactory factory =
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(
                Collections.singletonList(new RequestResponseLoggingInterceptor()));
        restTemplate.setUriTemplateHandler(
                new DefaultUriBuilderFactory(xrefRDFConfigProperties.getRequestUrl()));
        return restTemplate;
    }
}
