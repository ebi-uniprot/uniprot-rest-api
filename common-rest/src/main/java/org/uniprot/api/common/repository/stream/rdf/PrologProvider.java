package org.uniprot.api.common.repository.stream.rdf;

import java.util.List;

import org.springframework.stereotype.Component;
import org.uniprot.api.rest.service.NTriplesPrologs;
import org.uniprot.api.rest.service.RdfService;

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
    public static final String PREFIX_BASE = "@base";
    public static final String PREFIX_PREFIX = "@prefix";

    public String getProLog(
            List<String> firstBatch,
            RdfServiceFactory rdfServiceFactory,
            String dataType,
            String format) {
        RdfService<String> rdfService;
        switch (format) {
            case RDF:
            case TURTLE:
                rdfService = rdfServiceFactory.getRdfService(dataType, format);
                return rdfService.getProlog(firstBatch);
            case N_TRIPLES:
                return NTriplesPrologs.N_TRIPLES_COMMON_PROLOG;
            default:
                throw new IllegalArgumentException(String.format("Unsupported format %s", format));
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
