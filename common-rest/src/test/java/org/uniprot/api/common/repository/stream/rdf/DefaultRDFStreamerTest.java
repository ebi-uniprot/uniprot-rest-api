package org.uniprot.api.common.repository.stream.rdf;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.jodah.failsafe.RetryPolicy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.stream.document.DefaultDocumentIdStream;
import org.uniprot.api.common.repository.stream.document.TestDocument;
import org.uniprot.api.rest.service.RDFService;

/**
 * @author sahmad
 * @created 27/01/2021
 */
@ExtendWith(MockitoExtension.class)
class DefaultRDFStreamerTest {
    @Mock private SolrQueryRepository<TestDocument> repository;
    @Mock private RestTemplate restTemplate;

    static final String SAMPLE_RDF =
            "<?xml version='1.0' encoding='UTF-8'?>\n"
                    + "<rdf:RDF>\n"
                    + "    <owl:Ontology rdf:about=\"\">\n"
                    + "        <owl:imports rdf:resource=\"http://purl.uniprot.org/core/\"/>\n"
                    + "    </owl:Ontology>\n"
                    + "    <sample>text</sample>\n"
                    + "    <anotherSample>text2</anotherSample>\n"
                    + "    <someMore>text3</someMore>\n"
                    + "</rdf:RDF>";

    static final String RDF_PRELOG = "<?xml version='1.0' encoding='UTF-8'?>\n" + "<rdf:RDF>";

    @Test
    void idsToRDFStoreStream() {
        // when
        SolrRequest solrRequest = SolrRequest.builder().query("*:*").rows(5).totalRows(5).build();
        TestDocument doc1 = new TestDocument("1", "name1");
        TestDocument doc2 = new TestDocument("2", "name2");
        TestDocument doc3 = new TestDocument("3", "name3");
        TestDocument doc4 = new TestDocument("4", "name4");
        TestDocument doc5 = new TestDocument("5", "name5");
        when(repository.getAll(solrRequest)).thenReturn(Stream.of(doc1, doc2, doc3, doc4, doc5));

        DefaultDocumentIdStream<TestDocument> idStream =
                DefaultDocumentIdStream.<TestDocument>builder()
                        .documentToId(TestDocument::getDocumentId)
                        .repository(repository)
                        .build();

        when(restTemplate.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());
        when(restTemplate.getForObject(any(), any())).thenReturn(SAMPLE_RDF);

        RDFService<String> rdfService = new RDFService<>(restTemplate, String.class);

        DefaultRDFStreamer.DefaultRDFStreamerBuilder<TestDocument> builder =
                DefaultRDFStreamer.builder();
        builder.idStream(idStream).rdfFetchRetryPolicy(new RetryPolicy<>().withMaxRetries(3));
        builder.rdfProlog(RDF_PRELOG).rdfBatchSize(2).rdfService(rdfService);
        // then
        DefaultRDFStreamer<TestDocument> rdfStreamer = builder.build();
        Stream<String> rdfStream = rdfStreamer.idsToRDFStoreStream(solrRequest);
        String rdfString = rdfStream.collect(Collectors.joining());
        // 3 batches
        Assertions.assertEquals(
                "<?xml version='1.0' encoding='UTF-8'?>\n"
                        + "<rdf:RDF>\n"
                        + "    <sample>text</sample>\n"
                        + "    <anotherSample>text2</anotherSample>\n"
                        + "    <someMore>text3</someMore>\n"
                        + "\n"
                        + "    <sample>text</sample>\n"
                        + "    <anotherSample>text2</anotherSample>\n"
                        + "    <someMore>text3</someMore>\n"
                        + "\n"
                        + "    <sample>text</sample>\n"
                        + "    <anotherSample>text2</anotherSample>\n"
                        + "    <someMore>text3</someMore>\n"
                        + "</rdf:RDF>",
                rdfString);
    }
}
