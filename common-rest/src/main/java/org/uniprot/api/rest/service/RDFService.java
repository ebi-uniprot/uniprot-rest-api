package org.uniprot.api.rest.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;
import org.uniprot.store.datastore.common.StoreService;

public class RDFService<T> implements StoreService<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RDFService.class);
    private static final String QUERY_STR = "query";
    private static final String FORMAT_STR = "format";
    private static final String RDF_STR = "rdf";
    private static final String ID_COLON_STR = "id:";
    private static final String OR_DELIMITER_STR = " or ";
    public static final String UNIPROT_RDF_PROLOG =
            "<?xml version='1.0' encoding='UTF-8'?>\n"
                    + "<rdf:RDF xml:base=\"http://purl.uniprot.org/uniprot/\" xmlns=\"http://purl.uniprot.org/core/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:skos=\"http://www.w3.org/2004/02/skos/core#\" xmlns:bibo=\"http://purl.org/ontology/bibo/\" xmlns:foaf=\"http://xmlns.com/foaf/0.1/\" xmlns:void=\"http://rdfs.org/ns/void#\" xmlns:sd=\"http://www.w3.org/ns/sparql-service-description#\" xmlns:faldo=\"http://biohackathon.org/resource/faldo#\">\n"
                    + "    <owl:Ontology rdf:about=\"http://purl.uniprot.org/uniprot/\">\n"
                    + "        <owl:imports rdf:resource=\"http://purl.uniprot.org/core/\"/>\n"
                    + "    </owl:Ontology>";
    public static final String UNIREF_RDF_PROLOG =
            "<?xml version='1.0' encoding='UTF-8'?>\n"
                    + "<rdf:RDF xmlns=\"http://purl.uniprot.org/core/\" xmlns:isoform=\"http://purl.uniprot.org/isoforms/\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:taxon=\"http://purl.uniprot.org/taxonomy/\" xmlns:uniparc=\"http://purl.uniprot.org/uniparc/\" xmlns:uniprot=\"http://purl.uniprot.org/uniprot/\" xmlns:uniref=\"http://purl.uniprot.org/uniref/\">\n"
                    + "<owl:Ontology rdf:about=\"\">\n"
                    + "<owl:imports rdf:resource=\"http://purl.uniprot.org/core/\"/>\n"
                    + "</owl:Ontology>";

    public static final String UNIPARC_RDF_PROLOG =
            "<?xml version='1.0' encoding='UTF-8'?>\n"
                    + "<rdf:RDF xmlns=\"http://purl.uniprot.org/core/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:embl-cds=\"http://purl.uniprot.org/embl-cds/\" xmlns:ensembl=\"http://rdf.ebi.ac.uk/resource/ensembl/\" xmlns:faldo=\"http://biohackathon.org/resource/faldo#\" xmlns:isoform=\"http://purl.uniprot.org/isoforms/\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:skos=\"http://www.w3.org/2004/02/skos/core#\" xmlns:ssmRegion=\"http://purl.uniprot.org/signatureSequenceMatch/\" xmlns:taxon=\"http://purl.uniprot.org/taxonomy/\" xmlns:uniparc=\"http://purl.uniprot.org/uniparc/\" xmlns:uniprot=\"http://purl.uniprot.org/uniprot/\">\n"
                    + "<owl:Ontology rdf:about=\"\">\n"
                    + "<owl:imports rdf:resource=\"http://purl.uniprot.org/core/\"/>\n"
                    + "</owl:Ontology>";

    public static final String RDF_CLOSE_TAG = "</rdf:RDF>";
    private static final String OWL_CLOSE_TAG = "</owl:Ontology>";
    private Class<T> clazz;
    private RestTemplate restTemplate;

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
        if (Objects.nonNull(rdfXML)) {
            T rdfResponse = convertRDFForStreaming(rdfXML);
            return Arrays.asList(rdfResponse);
        } else {
            return new ArrayList<>();
        }
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
        URI requestUri = uriBuilder.build();

        T rdfXML = restTemplate.getForObject(requestUri, this.clazz);

        return rdfXML;
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
     * @param rdfXML
     * @return
     */
    private T convertRDFForStreaming(T rdfXML) {
        T rdfResponse = rdfXML;
        if (this.clazz == String.class) {
            int indexOfCloseTag = ((String) rdfXML).indexOf(RDF_CLOSE_TAG);
            int endIndexOfOwlOntology =
                    ((String) rdfXML).indexOf(OWL_CLOSE_TAG) + OWL_CLOSE_TAG.length();
            // get everything between "</owl:Ontology>" and "</rdf:RDF>"
            rdfResponse = (T) ((String) rdfXML).substring(endIndexOfOwlOntology, indexOfCloseTag);
        }
        return rdfResponse;
    }
}
