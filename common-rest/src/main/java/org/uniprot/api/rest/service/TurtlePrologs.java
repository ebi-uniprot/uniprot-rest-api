package org.uniprot.api.rest.service;

public class TurtlePrologs {

    public static final String UNIPROT_PROLOG =
            "@base <http://purl.uniprot.org/uniprot/> .\n"
                    + "@prefix annotation: <http://purl.uniprot.org/annotation/> .\n"
                    + "@prefix citation: <http://purl.uniprot.org/citations/> .\n"
                    + "@prefix dcterms: <http://purl.org/dc/terms/> .\n"
                    + "@prefix disease: <http://purl.uniprot.org/diseases/> .\n"
                    + "@prefix ECO: <http://purl.obolibrary.org/obo/ECO_> .\n"
                    + "@prefix enzyme: <http://purl.uniprot.org/enzyme/> .\n"
                    + "@prefix faldo: <http://biohackathon.org/resource/faldo#> .\n"
                    + "@prefix foaf: <http://xmlns.com/foaf/0.1/> .\n"
                    + "@prefix go: <http://purl.obolibrary.org/obo/GO_> .\n"
                    + "@prefix isoform: <http://purl.uniprot.org/isoforms/> .\n"
                    + "@prefix keyword: <http://purl.uniprot.org/keywords/> .\n"
                    + "@prefix location: <http://purl.uniprot.org/locations/> .\n"
                    + "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
                    + "@prefix position: <http://purl.uniprot.org/position/> .\n"
                    + "@prefix pubmed: <http://purl.uniprot.org/pubmed/> .\n"
                    + "@prefix range: <http://purl.uniprot.org/range/> .\n"
                    + "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
                    + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
                    + "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
                    + "@prefix taxon: <http://purl.uniprot.org/taxonomy/> .\n"
                    + "@prefix tissue: <http://purl.uniprot.org/tissues/> .\n"
                    + "@prefix up: <http://purl.uniprot.org/core/> .\n"
                    + "@prefix database: <http://purl.uniprot.org/database/> .\n"
                    + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .";
    public static final String UNIREF_PROLOG =
            "@base <http://purl.uniprot.org/uniref/> .\n"
                    + "@prefix isoform: <http://purl.uniprot.org/isoforms/> .\n"
                    + "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
                    + "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
                    + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
                    + "@prefix taxon: <http://purl.uniprot.org/taxonomy/> .\n"
                    + "@prefix uniparc: <http://purl.uniprot.org/uniparc/> .\n"
                    + "@prefix uniprot: <http://purl.uniprot.org/uniprot/> .\n"
                    + "@prefix uniref: <http://purl.uniprot.org/uniref/> .\n"
                    + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .";

    public static final String UNIPARC_PROLOG =
            "@base <http://purl.uniprot.org/uniparc/> .\n"
                    + "@prefix dcterms: <http://purl.org/dc/terms/> .\n"
                    + "@prefix embl-cds: <http://purl.uniprot.org/embl-cds/> .\n"
                    + "@prefix ensembl: <http://rdf.ebi.ac.uk/resource/ensembl/> .\n"
                    + "@prefix faldo: <http://biohackathon.org/resource/faldo#> .\n"
                    + "@prefix isoform: <http://purl.uniprot.org/isoforms/> .\n"
                    + "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
                    + "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
                    + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
                    + "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
                    + "@prefix ssmRegion: <http://purl.uniprot.org/signatureSequenceMatch/> .\n"
                    + "@prefix taxon: <http://purl.uniprot.org/taxonomy/> .\n"
                    + "@prefix uniparc: <http://purl.uniprot.org/uniparc/> .\n"
                    + "@prefix uniprot: <http://purl.uniprot.org/uniprot/> .\n"
                    + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .";

    public static final String XREF_PROLOG =
            "@base <http://purl.uniprot.org/database/> .\n"
                    + "@prefix dcterms: <http://purl.org/dc/terms/> .\n"
                    + "@prefix foaf: <http://xmlns.com/foaf/0.1/> .\n"
                    + "@prefix journal: <http://purl.uniprot.org/journals/> .\n"
                    + "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
                    + "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
                    + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
                    + "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
                    + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .";

    public static final String DISEASE_PROLOG =
            "@base <http://purl.uniprot.org/diseases/> .\n"
                    + "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
                    + "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
                    + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
                    + "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
                    + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .";
    public static final String KEYWORD_PROLOG =
            "@base <http://purl.uniprot.org/keywords/> .\n"
                    + "@prefix obo: <http://purl.obolibrary.org/obo/> .\n"
                    + "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
                    + "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
                    + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
                    + "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
                    + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .";

    public static final String LITERATURE_PROLOG =
            "@base <http://purl.uniprot.org/citations/> .\n"
                    + "@prefix bibo: <http://purl.org/ontology/bibo/> .\n"
                    + "@prefix busco: <http://busco.ezlab.org/schema#> .\n"
                    + "@prefix dcterms: <http://purl.org/dc/terms/> .\n"
                    + "@prefix foaf: <http://xmlns.com/foaf/0.1/> .\n"
                    + "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
                    + "@prefix pubmed: <http://purl.uniprot.org/pubmed/> .\n"
                    + "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
                    + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
                    + "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
                    + "@prefix up: <http://purl.uniprot.org/core/> .\n"
                    + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .";

    public static final String SUBCELLULAR_LOCATION_PROLOG =
            "@base <http://purl.uniprot.org/locations/> .\n"
                    + "@prefix dcterms: <http://purl.org/dc/terms/> .\n"
                    + "@prefix foaf: <http://xmlns.com/foaf/0.1/> .\n"
                    + "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
                    + "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
                    + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
                    + "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
                    + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .";

    public static final String TAXONOMY_PROLOG =
            "@base <http://purl.uniprot.org/taxonomy/> .\n"
                    + "@prefix bibo: <http://purl.org/ontology/bibo/> .\n"
                    + "@prefix busco: <http://busco.ezlab.org/schema#> .\n"
                    + "@prefix dcterms: <http://purl.org/dc/terms/> .\n"
                    + "@prefix foaf: <http://xmlns.com/foaf/0.1/> .\n"
                    + "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
                    + "@prefix pubmed: <http://purl.uniprot.org/pubmed/> .\n"
                    + "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
                    + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
                    + "@prefix skos: <http://www.w3.org/2004/02/skos/core#> .\n"
                    + "@prefix up: <http://purl.uniprot.org/core/> .\n"
                    + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .";

    private TurtlePrologs() {}
}
