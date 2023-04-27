package org.uniprot.api.common.repository.stream.rdf;

import org.springframework.stereotype.Component;
import org.uniprot.api.rest.service.NTriplesPrologs;
import org.uniprot.api.rest.service.RDFPrologs;
import org.uniprot.api.rest.service.TurtlePrologs;

@Component
public class PrologProvider {

    public static final String RDF = "rdf";
    public static final String TURTLE = "ttl";
    public static final String N_TRIPLES = "nt";
    public static final String UNIPROT = "uniprotkb";
    public static final String UNIREF = "uniref";
    public static final String UNIPARC = "uniparc";
    public static final String XREF = "databases";
    public static final String DISEASE = "diseases";
    public static final String KEYWORD = "keywords";
    public static final String LITERATURE = "citations";
    public static final String SUBCELLULAR = "locations";
    public static final String TAXONOMY = "taxonomy";

    public String getProLog(String dataType, String format) {
        switch (format) {
            case RDF:
                return getForRDF(dataType);
            case TURTLE:
                return getForTurtle(dataType);
            case N_TRIPLES:
                return NTriplesPrologs.N_TRIPLES_COMMON_PROLOG;
            default:
                throw new IllegalArgumentException(String.format("Unsupported format %s", format));
        }
    }

    private String getForRDF(String dataType) {
        switch (dataType) {
            case UNIPROT:
                return RDFPrologs.UNIPROT_PROLOG;
            case UNIREF:
                return RDFPrologs.UNIREF_PROLOG;
            case UNIPARC:
                return RDFPrologs.UNIPARC_PROLOG;
            case XREF:
                return RDFPrologs.XREF_PROLOG;
            case DISEASE:
                return RDFPrologs.DISEASE_PROLOG;
            case KEYWORD:
                return RDFPrologs.KEYWORD_PROLOG;
            case LITERATURE:
                return RDFPrologs.LITERATURE_PROLOG;
            case SUBCELLULAR:
                return RDFPrologs.SUBCELLULAR_LOCATION_PROLOG;
            case TAXONOMY:
                return RDFPrologs.TAXONOMY_PROLOG;
            default:
                throw new IllegalArgumentException(String.format("Invalid type %s", dataType));
        }
    }

    private String getForTurtle(String dataType) {
        switch (dataType) {
            case UNIPROT:
                return TurtlePrologs.UNIPROT_PROLOG;
            case UNIREF:
                return TurtlePrologs.UNIREF_PROLOG;
            case UNIPARC:
                return TurtlePrologs.UNIPARC_PROLOG;
            case XREF:
                return TurtlePrologs.XREF_PROLOG;
            case DISEASE:
                return TurtlePrologs.DISEASE_PROLOG;
            case KEYWORD:
                return TurtlePrologs.KEYWORD_PROLOG;
            case LITERATURE:
                return TurtlePrologs.LITERATURE_PROLOG;
            case SUBCELLULAR:
                return TurtlePrologs.SUBCELLULAR_LOCATION_PROLOG;
            case TAXONOMY:
                return TurtlePrologs.TAXONOMY_PROLOG;
            default:
                throw new IllegalArgumentException(String.format("Invalid type %s", dataType));
        }
    }

    public String getClosingTag(String format) {
        switch (format) {
            case RDF:
                return "</rdf:RDF>";
            case TURTLE:
            case N_TRIPLES:
                return "";
            default:
                throw new IllegalArgumentException(String.format("Unsupported format %s", format));
        }
    }
}
