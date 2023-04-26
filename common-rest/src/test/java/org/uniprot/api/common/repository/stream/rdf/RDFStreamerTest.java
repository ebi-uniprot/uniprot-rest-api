package org.uniprot.api.common.repository.stream.rdf;

import net.jodah.failsafe.RetryPolicy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.api.rest.service.RDFService;
import org.uniprot.api.rest.service.TagProvider;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author lgonzales
 * @since 28/01/2021
 */
@ExtendWith(MockitoExtension.class)
class RDFStreamerTest {
    public static final List<String> IDS = asList("a", "b");
    private static final String TYPE = "type";
    private static final String FORMAT = "format";
    private static final String RDF_PROLOG = "<?xml version='1.0' encoding='UTF-8'?>\n" + "<rdf:RDF>\n";
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
    private static final int BATCH_SIZE = 50;
    private static final String RDF_CLOSE_TAG = "</rdf:RDF>";
    private static final RetryPolicy<Object> RETRY_POLICY = new RetryPolicy<>().withMaxRetries(3);
    @Mock
    private PrologProvider prologProvider;
    @Mock
    private RDFServiceFactory rdfServiceFactory;
    @Mock
    private RDFService<String> rdfService;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private TagProvider tagProvider;
    RDFStreamer rdfStreamer;

    @BeforeEach
    void setUp() {
        rdfStreamer = new RDFStreamer(BATCH_SIZE, prologProvider, rdfServiceFactory, RETRY_POLICY);
        rdfService = new RDFService<>(tagProvider, restTemplate, String.class, TYPE, FORMAT);
        when(rdfServiceFactory.getRdfService(TYPE, FORMAT)).thenReturn(rdfService);
        when(restTemplate.getUriTemplateHandler()).thenReturn(new DefaultUriBuilderFactory());
        when(restTemplate.getForObject(any(), any())).thenReturn(SAMPLE_RDF);
        when(prologProvider.getProLog(TYPE, FORMAT)).thenReturn(RDF_PROLOG);
        when(prologProvider.getClosingTag(FORMAT)).thenReturn(RDF_CLOSE_TAG);
    }

    @Test
    void testExceptionFromServiceLayer() {
        Stream<String> idsStream = rdfStreamer.stream(IDS.stream(), TYPE, FORMAT);
        when(restTemplate.getForObject(any(), any())).thenThrow(RestClientException.class);
        Assertions.assertThrows(RestClientException.class, idsStream::count);
    }

    @Test
    void idsToRDFTupleStoreStream() {
        when(tagProvider.getStartingPosition(any(), eq(FORMAT))).thenReturn(169);
        when(tagProvider.getEndingPosition(any(), eq(FORMAT))).thenReturn(267);

        Stream<String> rdfStream = rdfStreamer.stream(IDS.stream(), TYPE, FORMAT);
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