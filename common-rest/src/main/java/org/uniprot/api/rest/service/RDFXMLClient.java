package org.uniprot.api.rest.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.net.URI;
import java.util.*;

@Slf4j
public class RDFXMLClient {
    private final TagProvider tagProvider;
    private final RestTemplate restTemplate;

    public RDFXMLClient(TagProvider tagProvider, RestTemplate rdfXmlTemplate) {
        this.tagProvider = tagProvider;
        this.restTemplate = rdfXmlTemplate;
    }

    public List<String> getEntries(Iterable<String> accessions, String type, String format) {
        List<String> allAccessions = new ArrayList<>();
        accessions.forEach(allAccessions::add);
        log.debug("RDF call for accessions : {}", allAccessions);
        String rdfXML = getEntriesByAccessions(allAccessions, type, format);
        if (Objects.nonNull(rdfXML)) {
            String rdfResponse = convertRDFForStreaming(rdfXML, format);
            return Collections.singletonList(rdfResponse);
        } else {
            return Collections.emptyList();
        }
    }

    public Optional<String> getEntry(String id, String type, String format) {
        return Optional.ofNullable(
                getEntriesByAccessions(Collections.singletonList(id), type, format));
    }

    private String getEntriesByAccessions(List<String> accessions, String type, String format) {
        String commaSeparatedIds = String.join(",", accessions);

        DefaultUriBuilderFactory handler =
                (DefaultUriBuilderFactory) restTemplate.getUriTemplateHandler();

        URI requestUri = handler.builder().build(type, format, commaSeparatedIds);

        return restTemplate.getForObject(requestUri, String.class);
    }

    /**
     * Start XML Tag Begin <?xml version='1.0' encoding='UTF-8'?> <rdf:RDF
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
    private String convertRDFForStreaming(String body, String format) {
        int startingPosition = tagProvider.getStartingPosition(body,format);
        int indexOfCloseTag = tagProvider.getEndingPosition(body,format);
        return body.substring(startingPosition, indexOfCloseTag);
    }
}
