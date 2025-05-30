package org.uniprot.api.rest.service;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class TagPositionProvider {
    public static final String RDF = "rdf";
    public static final String TURTLE = "ttl";
    public static final String N_TRIPLES = "nt";
    public static final String RDF_LAST_HEADER = "</owl:Ontology>";
    public static final String TURTLE_LAST_HEADER =
            "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .";
    public static final String RDF_CLOSING_TAG = "</rdf:RDF>";

    /**
     * Example: Start XML Tag Begin <?xml version='1.0' encoding='UTF-8'?> <rdf:RDF
     * xml:base="http://purl.uniprot.org/uniprot/" xmlns="http://purl.uniprot.org/core/"
     * xmlns:dcterms="http://purl.org/dc/terms/"
     * xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
     * xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:owl="http://www.w3.org/2002/07/owl#"
     * xmlns:skos="http://www.w3.org/2004/02/skos/core#" xmlns:bibo="http://purl.org/ontology/bibo/"
     * xmlns:foaf="http://xmlns.com/foaf/0.1/" xmlns:void="http://rdfs.org/ns/void#"
     * xmlns:sd="http://www.w3.org/ns/sparql-service-description#"
     * xmlns:faldo="http://biohackathon.org/resource/faldo#"> <owl:Ontology
     * rdf:about="http://purl.uniprot.org/uniprot/"> <owl:imports
     * rdf:resource="http://purl.uniprot.org/core/"/> </owl:Ontology> Start Tag End
     *
     * @param body
     * @param format
     * @return
     */
    public int getStartingPosition(String body, String format) {
        switch (format) {
            case RDF:
                return getBodyStartIndexAfterHeader(body, RDF_LAST_HEADER);
            case TURTLE:
                return getBodyStartIndexAfterHeader(body, TURTLE_LAST_HEADER);
            case N_TRIPLES:
                return 0;
            default:
                throw new IllegalArgumentException("Invalid format " + format);
        }
    }

    /**
     * Example:
     *
     * <p>End Tag </rdf:RDF> Logic- Convert RDF/XML response for streaming so that it is transmitted
     * as one big xml string. In the every batch, pass everything between RDF_PROLOG and
     * RDF_CLOSE_TAG(both exclusive) in any of the responses . The RDF_PROLOG will be added in the
     * beginning of stream and RDF_CLOSE_TAG will be added in the end of the stream. see
     * Stream.concat in StoreStreamer
     *
     * @param body
     * @param format
     * @return
     */
    public int getEndingPosition(String body, String format) {
        switch (format) {
            case RDF:
                int index = body.indexOf(RDF_CLOSING_TAG);
                if (index == -1) {
                    log.error("No RDF closing tag found in body {}", body);
                    throw new IllegalArgumentException(
                            "Unable to find RDF closing tag : " + RDF_CLOSING_TAG);
                }
                return index;
            case TURTLE:
            case N_TRIPLES:
                return body.length();
            default:
                throw new IllegalArgumentException("Invalid format " + format);
        }
    }

    private int getBodyStartIndexAfterHeader(String body, String lastHeader) {
        int indexOfLastHeader = body.indexOf(lastHeader);
        if (indexOfLastHeader == -1) {
            log.error("No last header found in body {}", body);
            throw new IllegalArgumentException("Unable to find last header : " + lastHeader);
        }
        int indexOfNewLine = body.indexOf('\n', indexOfLastHeader);
        if (indexOfNewLine == -1) {
            log.error("No new line found in the last header in body {}", body);
            throw new IllegalArgumentException(
                    "Unable to find new line in last header : " + lastHeader);
        }
        return indexOfNewLine + 1;
    }
}
