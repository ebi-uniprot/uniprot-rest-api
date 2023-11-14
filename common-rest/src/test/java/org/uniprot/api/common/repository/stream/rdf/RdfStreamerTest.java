package org.uniprot.api.common.repository.stream.rdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.jodah.failsafe.RetryPolicy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.api.rest.service.RdfService;
import org.uniprot.api.rest.service.TagPositionProvider;

/**
 * @author lgonzales
 * @since 28/01/2021
 */
@ExtendWith(MockitoExtension.class)
class RdfStreamerTest {
    public static final List<String> IDS = List.of("a", "b");
    private static final String DATA_TYPE = "dataType";
    private static final String FORMAT = "format";
    private static final String RDF_PROLOG =
            "<?xml version='1.0' encoding='UTF-8'?>\n" + "<rdf:RDF>\n";
    private static final String SAMPLE_RDF =
            "<?xml version='1.0' encoding='UTF-8'?>\n"
                    + "<rdf:RDF>\n"
                    + "    <owl:Ontology rdf:about=\"\">\n"
                    + "        <owl:imports rdf:resource=\"http://purl.uniprot.org/core/\"/>\n"
                    + "    </owl:Ontology>\n"
                    + "    <sample>text</sample>\n"
                    + "    <anotherSample>text2</anotherSample>\n"
                    + "    <someMore>text3</someMore>\n"
                    + "</rdf:RDF>";
    private static final int BATCH_SIZE = 2;
    private static final String RDF_CLOSE_TAG = "</rdf:RDF>";
    private static final RetryPolicy<Object> RETRY_POLICY = new RetryPolicy<>().withMaxRetries(3);
    private static final String EMPTY_RESPONSE =
            "<?xml version='1.0' encoding='UTF-8'?>\n" + "<rdf:RDF>\n" + "</rdf:RDF>";
    @Mock private PrologProvider prologProvider;
    @Mock private RdfServiceFactory rdfServiceFactory;
    @Mock private RdfService<String> rdfService;
    @Mock private RestTemplate restTemplate;
    @Mock private TagPositionProvider tagPositionProvider;
    @Mock private RdfEntryCountProvider rdfEntryCountProvider;
    RdfStreamer rdfStreamer;

    @BeforeEach
    void setUp() {
        rdfStreamer =
                new RdfStreamer(
                        BATCH_SIZE,
                        prologProvider,
                        rdfServiceFactory,
                        RETRY_POLICY,
                        rdfEntryCountProvider);
        rdfService =
                new RdfService<>(
                        tagPositionProvider, restTemplate, String.class, DATA_TYPE, FORMAT);
        when(rdfServiceFactory.getRdfService(DATA_TYPE, FORMAT)).thenReturn(rdfService);
        when(restTemplate.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());
        when(restTemplate.getForObject(any(), any())).thenReturn(SAMPLE_RDF);
        when(prologProvider.getProLog(DATA_TYPE, FORMAT)).thenReturn(RDF_PROLOG);
        when(prologProvider.getClosingTag(FORMAT)).thenReturn(RDF_CLOSE_TAG);
    }

    @Test
    void stream_forSingleBatch() {
        when(tagPositionProvider.getStartingPosition(any(), eq(FORMAT))).thenReturn(169);
        when(tagPositionProvider.getEndingPosition(any(), eq(FORMAT))).thenReturn(267);

        Stream<String> rdfStream = rdfStreamer.stream(IDS.stream(), DATA_TYPE, FORMAT);
        String rdfString = rdfStream.collect(Collectors.joining());

        // 1 batch
        assertEquals(
                "<?xml version='1.0' encoding='UTF-8'?>\n"
                        + "<rdf:RDF>\n"
                        + "    <sample>text</sample>\n"
                        + "    <anotherSample>text2</anotherSample>\n"
                        + "    <someMore>text3</someMore>\n"
                        + "</rdf:RDF>",
                rdfString);
    }

    @Test
    void stream_forMultipleBatches() {
        when(tagPositionProvider.getStartingPosition(any(), eq(FORMAT))).thenReturn(169);
        when(tagPositionProvider.getEndingPosition(any(), eq(FORMAT))).thenReturn(267);

        Stream<String> rdfStream =
                rdfStreamer.stream(Stream.of("a", "b", "c", "d", "e"), DATA_TYPE, FORMAT);

        String rdfString = rdfStream.collect(Collectors.joining());
        // 3 batches
        assertEquals(
                "<?xml version='1.0' encoding='UTF-8'?>\n"
                        + "<rdf:RDF>\n"
                        + "    <sample>text</sample>\n"
                        + "    <anotherSample>text2</anotherSample>\n"
                        + "    <someMore>text3</someMore>"
                        + "\n"
                        + "    <sample>text</sample>\n"
                        + "    <anotherSample>text2</anotherSample>\n"
                        + "    <someMore>text3</someMore>"
                        + "\n"
                        + "    <sample>text</sample>\n"
                        + "    <anotherSample>text2</anotherSample>\n"
                        + "    <someMore>text3</someMore>\n"
                        + "</rdf:RDF>",
                rdfString);
    }

    @Test
    void stream_whenRemoteServerResponseIsEmpty() {
        Stream<String> rdfStream = rdfStreamer.stream(IDS.stream(), DATA_TYPE, FORMAT);

        String rdfString = rdfStream.collect(Collectors.joining());
        assertEquals(EMPTY_RESPONSE, rdfString);
    }

    @Test
    void stream_whenRemoteServerThrowsException() {
        Stream<String> idsStream = rdfStreamer.stream(IDS.stream(), DATA_TYPE, FORMAT);
        when(restTemplate.getForObject(any(), any())).thenThrow(RestClientException.class);
        assertThrows(RestClientException.class, idsStream::count);
    }
}
