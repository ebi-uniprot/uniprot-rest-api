package org.uniprot.api.rest.service;

/**
 * @author sahmad
 * @created 01/02/2021 This file keeps the RDF response prefix for each data type
 */
public class RDFPrologs {

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
    public static final String KEYWORD_PROLOG =
            "<?xml version='1.0' encoding='UTF-8'?>\n"
                    + "<rdf:RDF xmlns=\"http://purl.uniprot.org/core/\" xmlns:obo=\"http://purl.obolibrary.org/obo/\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:skos=\"http://www.w3.org/2004/02/skos/core#\">\n"
                    + "<owl:Ontology rdf:about=\"\">\n"
                    + "<owl:imports rdf:resource=\"http://purl.uniprot.org/core/\"/>\n"
                    + "</owl:Ontology>";

    public static final String LITERATURE_PROLOG =
            "<?xml version='1.0' encoding='UTF-8'?>\n"
                    + "<rdf:RDF xmlns=\"http://purl.uniprot.org/core/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:foaf=\"http://xmlns.com/foaf/0.1/\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:pubmed=\"http://purl.uniprot.org/pubmed/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:skos=\"http://www.w3.org/2004/02/skos/core#\">\n"
                    + "<owl:Ontology rdf:about=\"\">\n"
                    + "<owl:imports rdf:resource=\"http://purl.uniprot.org/core/\"/>\n"
                    + "</owl:Ontology>";

    public static final String SUBCELLULAR_LOCATION_PROLOG =
            "<?xml version='1.0' encoding='UTF-8'?>\n"
                    + "<rdf:RDF xmlns=\"http://purl.uniprot.org/core/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:foaf=\"http://xmlns.com/foaf/0.1/\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:skos=\"http://www.w3.org/2004/02/skos/core#\">\n"
                    + "<owl:Ontology rdf:about=\"\">\n"
                    + "<owl:imports rdf:resource=\"http://purl.uniprot.org/core/\"/>\n"
                    + "</owl:Ontology>";

    public static final String TAXONOMY_PROLOG =
            "<?xml version='1.0' encoding='UTF-8'?>\n"
                    + "<rdf:RDF xml:base=\"http://purl.uniprot.org/taxonomy/\" xmlns=\"http://purl.uniprot.org/core/\" xmlns:foaf=\"http://xmlns.com/foaf/0.1/\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:skos=\"http://www.w3.org/2004/02/skos/core#\">\n"
                    + "<owl:Ontology rdf:about=\"http://purl.uniprot.org/taxonomy/\">\n"
                    + "<owl:imports rdf:resource=\"http://purl.uniprot.org/core/\"/>\n"
                    + "</owl:Ontology>";

    private RDFPrologs() {}
}
