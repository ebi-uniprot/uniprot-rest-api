package org.uniprot.api.rest.service;

/**
 * @author sahmad
 * @created 01/02/2021 This file keeps the RDF response prefix for each data type
 */
public class RDFPrologs {
    public static final String XREF_PROLOG =
            "<?xml version='1.0' encoding='UTF-8'?>\n"
                    + "<rdf:RDF xmlns=\"http://purl.uniprot.org/core/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:foaf=\"http://xmlns.com/foaf/0.1/\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:skos=\"http://www.w3.org/2004/02/skos/core#\">\n"
                    + "<owl:Ontology rdf:about=\"\">\n"
                    + "<owl:imports rdf:resource=\"http://purl.uniprot.org/core/\"/>\n"
                    + "</owl:Ontology>";

    public static final String DISEASE_PROLOG =
            "<?xml version='1.0' encoding='UTF-8'?>\n"
                    + "<rdf:RDF xmlns=\"http://purl.uniprot.org/core/\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:skos=\"http://www.w3.org/2004/02/skos/core#\">\n"
                    + "<owl:Ontology rdf:about=\"\">\n"
                    + "<owl:imports rdf:resource=\"http://purl.uniprot.org/core/\"/>\n"
                    + "</owl:Ontology>";
}
