package org.uniprot.api.rest.service;

/**
 * @author sahmad
 * @created 01/02/2021 This file keeps the RDF response prefix for each data type
 */
public class RdfPrologs {

    public static final String UNIPROT_PROLOG =
            "<?xml version='1.0' encoding='UTF-8'?>\n"
                    + "<rdf:RDF xml:base=\"http://purl.uniprot.org/uniprot/\" xmlns=\"http://purl.uniprot.org/core/\" xmlns:ECO=\"http://purl.obolibrary.org/obo/ECO_\" xmlns:annotation=\"http://purl.uniprot.org/annotation/\" xmlns:citation=\"http://purl.uniprot.org/citations/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:disease=\"http://purl.uniprot.org/diseases/\" xmlns:enzyme=\"http://purl.uniprot.org/enzyme/\" xmlns:faldo=\"http://biohackathon.org/resource/faldo#\" xmlns:foaf=\"http://xmlns.com/foaf/0.1/\" xmlns:go=\"http://purl.obolibrary.org/obo/GO_\" xmlns:isoform=\"http://purl.uniprot.org/isoforms/\" xmlns:keyword=\"http://purl.uniprot.org/keywords/\" xmlns:location=\"http://purl.uniprot.org/locations/\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:position=\"http://purl.uniprot.org/position/\" xmlns:pubmed=\"http://purl.uniprot.org/pubmed/\" xmlns:range=\"http://purl.uniprot.org/range/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:skos=\"http://www.w3.org/2004/02/skos/core#\" xmlns:taxon=\"http://purl.uniprot.org/taxonomy/\" xmlns:tissue=\"http://purl.uniprot.org/tissues/\">\n"
                    + "<owl:Ontology rdf:about=\"http://purl.uniprot.org/uniprot/\">\n"
                    + "<owl:imports rdf:resource=\"http://purl.uniprot.org/core/\"/>\n"
                    + "</owl:Ontology>";
    public static final String UNIREF_PROLOG =
            "<?xml version='1.0' encoding='UTF-8'?>\n"
                    + "<rdf:RDF xmlns=\"http://purl.uniprot.org/core/\" xmlns:isoform=\"http://purl.uniprot.org/isoforms/\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:taxon=\"http://purl.uniprot.org/taxonomy/\" xmlns:uniparc=\"http://purl.uniprot.org/uniparc/\" xmlns:uniprot=\"http://purl.uniprot.org/uniprot/\" xmlns:uniref=\"http://purl.uniprot.org/uniref/\">\n"
                    + "<owl:Ontology rdf:about=\"\">\n"
                    + "<owl:imports rdf:resource=\"http://purl.uniprot.org/core/\"/>\n"
                    + "</owl:Ontology>";

    public static final String UNIPARC_PROLOG =
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

    private RdfPrologs() {}
}
