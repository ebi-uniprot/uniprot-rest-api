package org.uniprot.api.common.repository.stream.rdf;

import org.springframework.stereotype.Component;
import org.uniprot.api.rest.service.NTriplesPrologs;
import org.uniprot.api.rest.service.RdfPrologs;
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
                return getForRdf(dataType);
            case TURTLE:
                return getForTurtle(dataType);
            case N_TRIPLES:
                return NTriplesPrologs.N_TRIPLES_COMMON_PROLOG;
            default:
                throw new IllegalArgumentException(String.format("Unsupported format %s", format));
        }
    }

    private String getForRdf(String dataType) {
        switch (dataType) {
            case UNIPROT:
                return RdfPrologs.UNIPROT_PROLOG;
            case UNIREF:
                return RdfPrologs.UNIREF_PROLOG;
            case UNIPARC:
                return RdfPrologs.UNIPARC_PROLOG;
            case XREF:
                return RdfPrologs.XREF_PROLOG;
            case DISEASE:
                return RdfPrologs.DISEASE_PROLOG;
            case KEYWORD:
                return RdfPrologs.KEYWORD_PROLOG;
            case LITERATURE:
                return RdfPrologs.LITERATURE_PROLOG;
            case SUBCELLULAR:
                return RdfPrologs.SUBCELLULAR_LOCATION_PROLOG;
            case TAXONOMY:
                return RdfPrologs.TAXONOMY_PROLOG;
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
