package org.uniprot.api.common.repository.stream.rdf;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author lgonzales
 * @since 28/01/2021
 */
@ExtendWith(MockitoExtension.class)
class RDFStreamerTest {

    /*@Mock private RestTemplate restTemplate;

    @Mock private SolrQueryRepository<TestDocument> repository;

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
    void idsToRDFStoreDocumentStream() {
        // when
        SolrRequest solrRequest = SolrRequest.builder().query("*:*").rows(5).totalRows(5).build();
        TestDocument doc1 = new TestDocument("1", "name1");
        TestDocument doc2 = new TestDocument("2", "name2");
        TestDocument doc3 = new TestDocument("3", "name3");
        TestDocument doc4 = new TestDocument("4", "name4");
        TestDocument doc5 = new TestDocument("5", "name5");
        when(repository.getAll(solrRequest)).thenReturn(Stream.of(doc1, doc2, doc3, doc4, doc5));

        DocumentIdStream idStream =
                DefaultDocumentIdStream.<TestDocument>builder()
                        .documentToId(TestDocument::getDocumentId)
                        .repository(repository)
                        .build();

        when(restTemplate.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());
        when(restTemplate.getForObject(any(), any())).thenReturn(SAMPLE_RDF);

        RDFService<String> rdfService = new RDFService<>(restTemplate, String.class);

        RDFStreamer.RDFStreamerBuilder builder = RDFStreamer.builder();
        builder.idStream(idStream).rdfFetchRetryPolicy(new RetryPolicy<>().withMaxRetries(3));
        builder.rdfProlog(RDF_PRELOG).rdfBatchSize(2).rdfService(rdfService);
        // then
        RDFStreamer rdfStreamer = builder.build();
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

    @Test
    void testEmptyResponse() {
        when(repository.getAll(any())).thenReturn(Stream.empty());

        DocumentIdStream idStream =
                DefaultDocumentIdStream.<TestDocument>builder()
                        .documentToId(TestDocument::getDocumentId)
                        .repository(repository)
                        .build();

        RDFService<String> rdfService = new RDFService<>(restTemplate, String.class);

        RDFStreamer.RDFStreamerBuilder builder = RDFStreamer.builder();
        builder.idStream(idStream).rdfFetchRetryPolicy(new RetryPolicy<>().withMaxRetries(3));
        builder.rdfProlog(RDF_PRELOG).rdfBatchSize(2).rdfService(rdfService);
        // then
        RDFStreamer rdfStreamer = builder.build();
        Stream<String> rdfStream = rdfStreamer.idsToRDFStoreStream(SolrRequest.builder().build());
        String rdfString = rdfStream.collect(Collectors.joining());
        String emptyResponse = "<?xml version='1.0' encoding='UTF-8'?>\n" + "<rdf:RDF></rdf:RDF>";
        Assertions.assertEquals(emptyResponse, rdfString);
    }

    @Test
    void testExceptionFromServiceLayer() {
        SolrRequest solrRequest = SolrRequest.builder().query("*:*").rows(5).totalRows(5).build();
        when(repository.getAll(solrRequest)).thenReturn(Stream.of(new TestDocument("1", "n")));
        DefaultDocumentIdStream<TestDocument> idStream =
                DefaultDocumentIdStream.<TestDocument>builder()
                        .documentToId(TestDocument::getDocumentId)
                        .repository(repository)
                        .build();
        // RestClientException
        when(restTemplate.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());
        when(restTemplate.getForObject(any(), any())).thenThrow(RestClientException.class);
        RDFService<String> rdfService = new RDFService<>(restTemplate, String.class);

        RDFStreamer.RDFStreamerBuilder builder = RDFStreamer.builder();
        builder.idStream(idStream).rdfFetchRetryPolicy(new RetryPolicy<>().withMaxRetries(3));
        builder.rdfProlog(RDF_PRELOG).rdfBatchSize(2).rdfService(rdfService);
        // then
        RDFStreamer rdfStreamer = builder.build();
        Stream<String> idsStream = rdfStreamer.idsToRDFStoreStream(solrRequest);
        Assertions.assertThrows(RestClientException.class, () -> idsStream.count());
    }

    @Test
    void idsToRDFTupleStoreStream() {
        // when
        SolrRequest solrRequest = SolrRequest.builder().query("*:*").rows(5).totalRows(5).build();
        List<String> ids = asList("a", "b");
        TupleStream tupleStream = TupleStreamUtils.tupleStream(ids);
        TupleStreamTemplate mockTupleStreamTemplate = Mockito.mock(TupleStreamTemplate.class);
        when(mockTupleStreamTemplate.create(ArgumentMatchers.any())).thenReturn(tupleStream);
        StreamerConfigProperties streamConfig = new StreamerConfigProperties();
        streamConfig.setIdFieldName("id");
        streamConfig.setStoreBatchSize(10);

        DocumentIdStream idStream =
                TupleStreamDocumentIdStream.builder()
                        .tupleStreamTemplate(mockTupleStreamTemplate)
                        .streamConfig(streamConfig)
                        .build();

        when(restTemplate.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());
        when(restTemplate.getForObject(any(), any())).thenReturn(SAMPLE_RDF);

        RDFService<String> rdfService = new RDFService<>(restTemplate, String.class);

        RDFStreamer.RDFStreamerBuilder builder = RDFStreamer.builder();
        builder.idStream(idStream).rdfFetchRetryPolicy(new RetryPolicy<>().withMaxRetries(3));
        builder.rdfProlog(RDF_PRELOG).rdfBatchSize(2).rdfService(rdfService);
        // then
        RDFStreamer rdfStreamer = builder.build();
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
    }*/
}
