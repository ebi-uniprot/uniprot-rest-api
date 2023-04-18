package org.uniprot.api.rest.service;

import org.springframework.stereotype.Component;

@Component
public class TagProvider {
    public static final String RDF = "rdf";
    public static final String TTL = "ttl";
    public static final String NT = "nt";

    public int getStartingPosition(String body, String format) {
        switch (format) {
            case RDF:
                String rdfStartingTag = "</owl:Ontology>";
                return body.indexOf(rdfStartingTag) + rdfStartingTag.length();
            case TTL:
                String ttlStartingTag = "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .";
                return body.indexOf(ttlStartingTag) + ttlStartingTag.length();
            case NT:
                return 0;
        }
        throw new RuntimeException("Invalid format " + format);
    }

    public int getEndingPosition(String body, String format) {
        switch (format) {
            case RDF:
                return body.indexOf("</rdf:RDF>");
            case TTL:
            case NT:
                return body.length();
        }
        throw new RuntimeException("Invalid format " + format);
    }
}
