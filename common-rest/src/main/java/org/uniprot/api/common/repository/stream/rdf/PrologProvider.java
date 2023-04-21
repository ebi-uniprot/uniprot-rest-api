package org.uniprot.api.common.repository.stream.rdf;

import org.springframework.stereotype.Component;
import org.uniprot.api.rest.service.NTPrologs;
import org.uniprot.api.rest.service.RDFPrologs;
import org.uniprot.api.rest.service.TTLPrologs;

@Component
public class PrologProvider {

    public static final String RDF = "rdf";
    public static final String TTL = "ttl";
    public static final String NT = "nt";
    public static final String UNIPROT = "uniprotkb";
    public static final String UNIREF = "uniref";
    public static final String UNIPARC = "uniparc";
    public static final String XREF = "databases";
    public static final String DISEASE = "diseases";
    public static final String KEYWORD = "keywords";
    public static final String LITERATURE = "citations";
    public static final String SUBCELLULAR = "locations";
    public static final String TAXONOMY = "taxonomy";

    public String getProLog(String type, String format) {
        switch (format) {
            case RDF:
                return getForRdf(type);
            case TTL:
                return getForTtl(type);
            case NT:
                return NTPrologs.NT_COMMON_PROLOG;
        }
        throw new IllegalArgumentException(String.format("Unsupported format %s", format));
    }

    private String getForRdf(String type) {
        switch (type) {
            case UNIPROT:
                return RDFPrologs.UNIPROT_RDF_PROLOG;
            case UNIREF:
                return RDFPrologs.UNIREF_RDF_PROLOG;
            case UNIPARC:
                return RDFPrologs.UNIPARC_RDF_PROLOG;
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
        }
        throw new IllegalArgumentException(String.format("Invalid type %s", type));
    }

    private String getForTtl(String type) {
        switch (type) {
            case UNIPROT:
                return TTLPrologs.UNIPROT_RDF_PROLOG;
            case UNIREF:
                return TTLPrologs.UNIREF_RDF_PROLOG;
            case UNIPARC:
                return TTLPrologs.UNIPARC_RDF_PROLOG;
            case XREF:
                return TTLPrologs.XREF_PROLOG;
            case DISEASE:
                return TTLPrologs.DISEASE_PROLOG;
            case KEYWORD:
                return TTLPrologs.KEYWORD_PROLOG;
            case LITERATURE:
                return TTLPrologs.LITERATURE_PROLOG;
            case SUBCELLULAR:
                return TTLPrologs.SUBCELLULAR_LOCATION_PROLOG;
            case TAXONOMY:
                return TTLPrologs.TAXONOMY_PROLOG;
        }
        throw new IllegalArgumentException(String.format("Invalid type %s", type));
    }

    public String getClosingTag(String format) {
        switch (format) {
            case RDF:
                return "</rdf:RDF>";
            case TTL:
            case NT:
                return "";
        }
        throw new IllegalArgumentException(String.format("Unsupported format %s", format));
    }
}
