package org.uniprot.api.rest.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TagPositionProviderTest {
    private TagPositionProvider provider;

    @BeforeEach
    void setUp() {
        provider = new TagPositionProvider();
    }

    @Test
    void testGetStartingPositionRDFSuccess() {
        String body = "<?xml...>\n<rdf:RDF>\n<owl:Ontology></owl:Ontology>\n<data>";
        int pos = provider.getStartingPosition(body, "rdf");
        assertEquals(body.indexOf("<data>"), pos);
    }

    @Test
    void testGetStartingPositionRDFMissingHeaderThrowsException() {
        String body = "<rdf:RDF>\n<owl:Ontology rdf:about=\"\"/>";
        IllegalArgumentException ex =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> provider.getStartingPosition(body, "rdf"));
        assertTrue(ex.getMessage().contains("Unable to find last header"));
    }

    @Test
    void testGetStartingPositionRDFMissingNewLineThrowsException() {
        String body = "<rdf:RDF><owl:Ontology></owl:Ontology>";
        body += TagPositionProvider.RDF_LAST_HEADER + "<rdf:Description>";
        String finalBody = body;
        IllegalArgumentException ex =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> provider.getStartingPosition(finalBody, "rdf"));
        assertTrue(ex.getMessage().contains("Unable to find new line"));
    }

    @Test
    void testGetStartingPositionTurtleSuccess() {
        String body =
                "@base <http://purl.uniprot.org/uniprot/> .\n"
                        + "@prefix annotation: <http://purl.uniprot.org/annotation/> .\n"
                        + "@prefix citation: <http://purl.uniprot.org/citations/> .\n"
                        + "@prefix dcterms: <http://purl.org/dc/terms/> .\n"
                        + "@prefix disease: <http://purl.uniprot.org/diseases/> .\n<some:Triple>";
        int pos = provider.getStartingPosition(body, "ttl");
        assertEquals(body.indexOf("<some:Triple>"), pos);
    }

    @Test
    void testGetStartingPositionTurtleFailureWithoutBody() {
        String body =
                "@base <http://purl.uniprot.org/uniprot/> .\n"
                        + "@prefix annotation: <http://purl.uniprot.org/annotation/> .\n"
                        + "@prefix citation: <http://purl.uniprot.org/citations/> .\n"
                        + "@prefix dcterms: <http://purl.org/dc/terms/> .\n"
                        + "@prefix disease: <http://purl.uniprot.org/diseases/> .\n";
        assertThrows(
                IllegalArgumentException.class, () -> provider.getStartingPosition(body, "ttl"));
    }

    @Test
    void testGetStartingPositionNTriplesReturnsZero() {
        int pos = provider.getStartingPosition("<subject> <predicate> <object> .", "nt");
        assertEquals(0, pos);
    }

    @Test
    void testGetStartingPositionInvalidFormatThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> provider.getStartingPosition("some content", "json"));
    }

    @Test
    void testGetEndingPositionRDFSuccess() {
        String body = "<rdf:RDF>\n<data/>\n</rdf:RDF>";
        int pos = provider.getEndingPosition(body, "rdf");
        assertEquals(body.indexOf("</rdf:RDF>"), pos);
    }

    @Test
    void testGetEndingPositionRDFMissingTagThrowsException() {
        String body = "<rdf:RDF>\n<data/>\n</rdf:NotRDF>";
        assertThrows(IllegalArgumentException.class, () -> provider.getEndingPosition(body, "rdf"));
    }

    @Test
    void testGetEndingPositionTurtle() {
        String body = "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n<some:Triple>";
        assertEquals(body.length(), provider.getEndingPosition(body, "ttl"));
    }

    @Test
    void testGetEndingPositionNTriples() {
        String body = "<subject> <predicate> <object> .";
        assertEquals(body.length(), provider.getEndingPosition(body, "nt"));
    }

    @Test
    void testGetEndingPositionInvalidFormatThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> provider.getEndingPosition("some body", "json"));
    }

    @Test
    void testGetStartingPositionRDFWithSpacesAfterHeaderAndNewline() {
        String body =
                "<?xml...>\n"
                        + "<rdf:RDF>\n"
                        + "</owl:Ontology>     \n"
                        + // spaces before newline
                        "<data/>";
        int pos = provider.getStartingPosition(body, "rdf");
        assertEquals(body.indexOf("<data/>"), pos);
    }

    @Test
    void testGetStartingPositionTurtleWithTabsAndSpacesAfterHeader() {
        String body = "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .     \n<other:Triple>";
        int pos = provider.getStartingPosition(body, "ttl");
        assertEquals(body.indexOf("<other:Triple>"), pos);
    }
}
