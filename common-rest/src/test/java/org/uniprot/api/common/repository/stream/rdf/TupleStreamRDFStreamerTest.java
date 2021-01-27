package org.uniprot.api.common.repository.stream.rdf;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.uniprot.api.common.repository.stream.rdf.DefaultRDFStreamerTest.RDF_PRELOG;
import static org.uniprot.api.common.repository.stream.rdf.DefaultRDFStreamerTest.SAMPLE_RDF;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.jodah.failsafe.RetryPolicy;

import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.stream.common.TupleStreamTemplate;
import org.uniprot.api.common.repository.stream.document.TupleStreamDocumentIdStream;
import org.uniprot.api.common.repository.stream.store.StreamerConfigProperties;
import org.uniprot.api.common.repository.stream.store.TupleStreamUtils;
import org.uniprot.api.rest.service.RDFService;

/**
 * @author sahmad
 * @created 27/01/2021
 */
@ExtendWith(MockitoExtension.class)
class TupleStreamRDFStreamerTest {
    @Mock private RestTemplate restTemplate;

    @Test
    void idsToRDFStoreStream() {
        // when
        SolrRequest solrRequest = SolrRequest.builder().query("*:*").rows(5).totalRows(5).build();
        List<String> ids = asList("a", "b");
        TupleStream tupleStream = TupleStreamUtils.tupleStream(ids);
        TupleStreamTemplate mockTupleStreamTemplate = Mockito.mock(TupleStreamTemplate.class);
        when(mockTupleStreamTemplate.create(ArgumentMatchers.any())).thenReturn(tupleStream);
        StreamerConfigProperties streamConfig = new StreamerConfigProperties();
        streamConfig.setIdFieldName("id");
        streamConfig.setStoreBatchSize(10);

        TupleStreamDocumentIdStream idStream =
                TupleStreamDocumentIdStream.builder()
                        .tupleStreamTemplate(mockTupleStreamTemplate)
                        .streamConfig(streamConfig)
                        .build();

        when(restTemplate.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());
        when(restTemplate.getForObject(any(), any())).thenReturn(SAMPLE_RDF);

        RDFService<String> rdfService = new RDFService<>(restTemplate, String.class);

        TupleStreamRDFStreamer.TupleStreamRDFStreamerBuilder builder =
                TupleStreamRDFStreamer.builder();
        builder.idStream(idStream).rdfFetchRetryPolicy(new RetryPolicy<>().withMaxRetries(3));
        builder.rdfProlog(RDF_PRELOG).rdfBatchSize(2).rdfService(rdfService);
        // then
        TupleStreamRDFStreamer rdfStreamer = builder.build();
        Stream<String> rdfStream = rdfStreamer.idsToRDFStoreStream(solrRequest);
        String rdfString = rdfStream.collect(Collectors.joining());

        // 1 batch
        Assertions.assertEquals(
                "<?xml version='1.0' encoding='UTF-8'?>\n"
                        + "<rdf:RDF>\n"
                        + "    <sample>text</sample>\n"
                        + "    <anotherSample>text2</anotherSample>\n"
                        + "    <someMore>text3</someMore>\n"
                        + "</rdf:RDF>",
                rdfString);
    }
}
