package org.uniprot.api.rest.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;
import org.uniprot.api.common.repository.stream.rdf.PrologProvider;

class RdfServiceTest {

    private TagPositionProvider tagPositionProvider;
    @Mock private RestTemplate restTemplate;
    @Mock private DefaultUriBuilderFactory uriFactory;
    @Mock private UriBuilder uriBuilder;

    private RdfService<String> rdfService;

    @BeforeEach
    void setup() {
        tagPositionProvider = new TagPositionProvider();
        MockitoAnnotations.openMocks(this);

        // Mock uriFactory behavior to return the uriBuilder when builder() is called
        when(restTemplate.getUriTemplateHandler()).thenReturn(uriFactory);
        when(uriFactory.builder()).thenReturn(uriBuilder);

        // Mock uriBuilder.build(...) to return a dummy URI
        try {
            when(uriBuilder.build(anyString(), anyString(), anyString()))
                    .thenReturn(new URI("http://dummy.uri"));
        } catch (Exception e) {
            fail("URI building failed: " + e.getMessage());
        }
    }

    @Test
    void testGetPrologRdfFormat() {
        String format = PrologProvider.RDF;
        String dataType = "type";
        List<String> ids = List.of("id1", "id2");

        String responseBody = RDF_PROLOG_BODY + RDF_CONTENT_BODY + RDF_ENDING_TAG;

        // Create RdfService with RDF format
        rdfService =
                new RdfService<>(tagPositionProvider, restTemplate, String.class, dataType, format);

        // Stub restTemplate to return the response body
        when(restTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(responseBody);

        String prolog = rdfService.getProlog(ids);

        // Expected prolog
        assertEquals(RDF_PROLOG_BODY, prolog);

        // Verify that URI was built correctly
        verify(uriBuilder).build(dataType, format, "id1,id2");
        verify(restTemplate).getForObject(any(URI.class), eq(String.class));
    }

    @Test
    void testGetPrologTurtleFormat() {
        String format = PrologProvider.TURTLE;
        String dataType = "type";
        List<String> ids = List.of("id1");

        String responseBody = TURTLE_PROLOG_BODY + TURTLE_CONTENT_BODY;

        rdfService =
                new RdfService<>(tagPositionProvider, restTemplate, String.class, dataType, format);

        when(restTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(responseBody);

        String prolog = rdfService.getProlog(ids);

        assertEquals(TURTLE_PROLOG_BODY, prolog);
    }

    @Test
    void testGetPrologNTFormat() {
        String format = PrologProvider.N_TRIPLES;
        String dataType = "type";
        List<String> ids = List.of("id1");
        String responseBody = "some content";

        rdfService =
                new RdfService<>(tagPositionProvider, restTemplate, String.class, dataType, format);

        when(restTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(responseBody);

        String prolog = rdfService.getProlog(ids);

        assertEquals("", prolog);
    }

    @Test
    void testGetPrologNullResponse() {
        String format = PrologProvider.RDF;
        String dataType = "type";
        List<String> ids = List.of("id1");

        RdfService<String> service =
                new RdfService<>(tagPositionProvider, restTemplate, String.class, dataType, format);
        when(restTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(null);

        String prolog = service.getProlog(ids);
        assertEquals("", prolog);
    }

    @Test
    void testGetPrologNonStringClass() {
        String format = PrologProvider.RDF;
        String dataType = "type";
        List<String> ids = List.of("id1");

        // Create with clazz other than String.class
        RdfService<Integer> service =
                new RdfService<>(
                        tagPositionProvider, restTemplate, Integer.class, dataType, format);

        // It should return empty string without calling restTemplate
        String prolog = service.getProlog(ids);
        assertEquals("", prolog);

        verifyNoInteractions(restTemplate);
    }

    @Test
    void testGetEntriesInRDFWithoutPrologAndEndingTag() {
        String format = PrologProvider.RDF;
        String dataType = "type";
        List<String> ids = List.of("id1", "id2");
        String responseBody = RDF_PROLOG_BODY + RDF_CONTENT_BODY + RDF_ENDING_TAG;

        // Create RdfService with RDF format
        rdfService =
                new RdfService<>(tagPositionProvider, restTemplate, String.class, dataType, format);

        // Stub restTemplate to return the response body
        when(restTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(responseBody);

        List<String> entries = rdfService.getEntries(ids);

        // Expected content without prolog and ending tag
        assertEquals(1, entries.size());
        assertEquals(RDF_CONTENT_BODY, entries.get(0));
    }

    @Test
    void testGetEntriesInTTLWithoutProlog() {
        String format = PrologProvider.TURTLE;
        String dataType = "type";
        List<String> ids = List.of("id1");
        String responseBody = TURTLE_PROLOG_BODY + TURTLE_CONTENT_BODY;

        // Create RdfService with ttl format
        rdfService =
                new RdfService<>(tagPositionProvider, restTemplate, String.class, dataType, format);

        // Stub restTemplate to return the response body
        when(restTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(responseBody);

        List<String> entries = rdfService.getEntries(ids);

        // Expected content without prolog and ending tag
        assertEquals(1, entries.size());
        assertEquals(TURTLE_CONTENT_BODY, entries.get(0));
    }

    @Test
    void testGetEntriesEmptyResponse() {
        String format = PrologProvider.TURTLE;
        String dataType = "type";
        List<String> ids = List.of("id1");

        // Create RdfService with ttl format
        rdfService =
                new RdfService<>(tagPositionProvider, restTemplate, String.class, dataType, format);

        // Stub restTemplate to return the response body
        when(restTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(null);

        List<String> entries = rdfService.getEntries(ids);

        assertTrue(entries.isEmpty());
    }

    @Test
    void testGetFullEntryInTTLWithoutProlog() {
        String format = PrologProvider.TURTLE;
        String dataType = "type";
        String id = "id1";
        String responseBody = TURTLE_PROLOG_BODY + TURTLE_CONTENT_BODY;

        // Create RdfService with ttl format
        rdfService =
                new RdfService<>(tagPositionProvider, restTemplate, String.class, dataType, format);

        // Stub restTemplate to return the response body
        when(restTemplate.getForObject(any(URI.class), eq(String.class))).thenReturn(responseBody);

        Optional<String> entry = rdfService.getEntry(id);

        // Expected content without prolog and ending tag
        assertTrue(entry.isPresent());
        assertEquals(responseBody, entry.get());
    }

    private static String RDF_PROLOG_BODY =
            "<?xml version='1.0' encoding='UTF-8'?>\n"
                    + "<rdf:RDF xml:base=\"http://purl.uniprot.org/uniprot/\" xmlns=\"http://purl.uniprot.org/core/\" xmlns:ECO=\"http://purl.obolibrary.org/obo/ECO_\" xmlns:annotation=\"http://purl.uniprot.org/annotation/\" xmlns:citation=\"http://purl.uniprot.org/citations/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:disease=\"http://purl.uniprot.org/diseases/\" xmlns:enzyme=\"http://purl.uniprot.org/enzyme/\" xmlns:faldo=\"http://biohackathon.org/resource/faldo#\" xmlns:foaf=\"http://xmlns.com/foaf/0.1/\" xmlns:go=\"http://purl.obolibrary.org/obo/GO_\" xmlns:isoform=\"http://purl.uniprot.org/isoforms/\" xmlns:keyword=\"http://purl.uniprot.org/keywords/\" xmlns:location=\"http://purl.uniprot.org/locations/\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:position=\"http://purl.uniprot.org/position/\" xmlns:pubmed=\"http://purl.uniprot.org/pubmed/\" xmlns:range=\"http://purl.uniprot.org/range/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:skos=\"http://www.w3.org/2004/02/skos/core#\" xmlns:taxon=\"http://purl.uniprot.org/taxonomy/\" xmlns:tissue=\"http://purl.uniprot.org/tissues/\">\n"
                    + "<owl:Ontology rdf:about=\"http://purl.uniprot.org/uniprot/\">\n"
                    + "<owl:imports rdf:resource=\"http://purl.uniprot.org/core/\"/>\n"
                    + "</owl:Ontology>\n";
    private static String RDF_CONTENT_BODY =
            "<rdf:Description rdf:about=\"P05067\">\n"
                    + "<rdf:type rdf:resource=\"http://purl.uniprot.org/core/Protein\"/>\n"
                    + "</rdf:Description>\n"
                    + "<rdf:Description rdf:about=\"P12345\">\n"
                    + "<rdf:type rdf:resource=\"http://purl.uniprot.org/core/Protein\"/>\n"
                    + "</rdf:Description>\n";
    private static String RDF_ENDING_TAG = "</rdf:RDF>";

    private static String TURTLE_PROLOG_BODY =
            "@base <http://purl.uniprot.org/uniparc/> .\n"
                    + "@prefix dcterms: <http://purl.org/dc/terms/> .\n"
                    + "@prefix embl-cds: <http://purl.uniprot.org/embl-cds/> .\n"
                    + "@prefix ensembl: <http://rdf.ebi.ac.uk/resource/ensembl/> .\n"
                    + "@prefix faldo: <http://biohackathon.org/resource/faldo#> .\n"
                    + "@prefix isoform: <http://purl.uniprot.org/isoforms/> .\n"
                    + "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
                    + "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
                    + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
                    + "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
                    + "@prefix ssmRegion: <http://purl.uniprot.org/signatureSequenceMatch/> .\n"
                    + "@prefix taxon: <http://purl.uniprot.org/taxonomy/> .\n"
                    + "@prefix uniparc: <http://purl.uniprot.org/uniparc/> .\n"
                    + "@prefix uniprot: <http://purl.uniprot.org/uniprot/> .\n"
                    + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n";
    private static String TURTLE_CONTENT_BODY =
            "<UPI0000000001#1> a faldo:ExactPosition ;\n"
                    + "  faldo:position 1 ;\n"
                    + "  faldo:reference <UPI0000000001> .";
}
