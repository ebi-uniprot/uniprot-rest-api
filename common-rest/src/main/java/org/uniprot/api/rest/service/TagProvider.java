package org.uniprot.api.rest.service;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TagProvider {
    private static final Map<String,String> startingTags = Map.of("rdf","</owl:Ontology>");
    private static final Map<String,String> closingTags = Map.of("rdf","</rdf:RDF>");

    public String getStartingTag(String format) {
        return startingTags.get(format);
    }

    public String getClosingTag(String format) {
        return closingTags.get(format);
    }
}
