package org.uniprot.api.rest.service;

import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.*;
import org.uniprot.store.datastore.common.StoreService;

public class RDFService<T> implements StoreService<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RDFService.class);
    private static final String QUERY_STR = "query";
    private static final String FORMAT_STR = "format";
    private static final String RDF_STR = "rdf";
    private static final String ID_COLON_STR = "id:";
    private static final String OR_DELIMITER_STR = " or ";
    public static final String RDF_CLOSE_TAG = "</rdf:RDF>";
    private static final String OWL_CLOSE_TAG = "</owl:Ontology>";
    private Class<T> clazz;
    private RestTemplate restTemplate;
    private boolean isFirstBatchRead;

    public RDFService(RestTemplate restTemplate, Class<T> clazz) {
        this.restTemplate = restTemplate;
        this.clazz = clazz;
    }

    @Override
    public List<T> getEntries(Iterable<String> accessions) {
        List<String> allAccessions = new ArrayList<>();
        accessions.forEach(acc -> allAccessions.add(acc));
        LOGGER.debug("RDF call for accessions : {}", allAccessions);
        T rdfXML = getEntriesByAccessions(allAccessions);
        T rdfResponse = convertRDFForStreaming(rdfXML);
        return Arrays.asList(rdfResponse);
    }

    @Override
    public String getStoreName() {
        return "RDF Store";
    }

    @Override
    public Optional<T> getEntry(String id) {
        return Optional.of(getEntriesByAccessions(Arrays.asList(id)));
    }

    private T getEntriesByAccessions(List<String> accessions) {
        // create query like id:P12345 or id:P54321 or ....
        String idQuery =
                accessions.stream()
                        .map(acc -> new StringBuilder(ID_COLON_STR).append(acc))
                        .collect(Collectors.joining(OR_DELIMITER_STR));

        DefaultUriBuilderFactory handler =
                (DefaultUriBuilderFactory) restTemplate.getUriTemplateHandler();

        UriBuilder uriBuilder =
                handler.builder().queryParam(QUERY_STR, idQuery).queryParam(FORMAT_STR, RDF_STR);

        T rdfXML = restTemplate.getForObject(uriBuilder.build(), this.clazz);

        return rdfXML;
    }

    /**
     * Start XML Tag Begin
     * <?xml version='1.0' encoding='UTF-8'?>
     * <rdf:RDF xml:base="http://purl.uniprot.org/uniprot/" xmlns="http://purl.uniprot.org/core/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:owl="http://www.w3.org/2002/07/owl#" xmlns:skos="http://www.w3.org/2004/02/skos/core#" xmlns:bibo="http://purl.org/ontology/bibo/" xmlns:foaf="http://xmlns.com/foaf/0.1/" xmlns:void="http://rdfs.org/ns/void#" xmlns:sd="http://www.w3.org/ns/sparql-service-description#" xmlns:faldo="http://biohackathon.org/resource/faldo#">
     * <owl:Ontology rdf:about="http://purl.uniprot.org/uniprot/">
     * <owl:imports rdf:resource="http://purl.uniprot.org/core/"/>
     * </owl:Ontology>
     * Start Tag End
     *
     * End Tag </rdf:RDF>
     * Logic-
     * Convert RDF/XML response for streaming so that it is transmitted as one big xml string
     * In the first batch, pass everything including the start xml tag not end tag. In the remaining batches, do not pass the start tag.
     * Do not pass end tag ("</rdf:RDF>") in any of the responses . The end tag will be added only
     * once in the end of the stream. see Stream.concat in StoreStreamer
     * @param rdfXML
     * @return
     */
    private T convertRDFForStreaming(T rdfXML) {
        T rdfResponse = rdfXML;
        if (this.clazz == String.class) {
            int indexOfCloseTag = ((String) rdfXML).indexOf(RDF_CLOSE_TAG);
            if (!this.isFirstBatchRead) { // get everything except before closing tag  </rdf:RDF> in
                // the first batch (starts with <?xml)
                this.isFirstBatchRead = true;
                rdfResponse = (T) ((String) rdfXML).substring(0, indexOfCloseTag);
            } else { // get everything between "</owl:Ontology>" and "</rdf:RDF>"
                int endIndexOfOwlOntology =
                        ((String) rdfXML).indexOf(OWL_CLOSE_TAG) + OWL_CLOSE_TAG.length();
                rdfResponse =
                        (T) ((String) rdfXML).substring(endIndexOfOwlOntology, indexOfCloseTag);
            }
        }
        return rdfResponse;
    }
}