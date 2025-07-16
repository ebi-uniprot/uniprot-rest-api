package org.uniprot.api.common.repository.stream.rdf;

import java.util.List;

import org.springframework.stereotype.Component;
import org.uniprot.api.rest.service.RdfService;

@Component
public class PrologProvider {

    public static final String RDF = "rdf";
    public static final String TURTLE = "ttl";
    public static final String N_TRIPLES = "nt";
    public static final String PREFIX_BASE = "@base";
    public static final String PREFIX_PREFIX = "@prefix";

    public String getProlog(
            List<String> firstBatch,
            RdfServiceFactory rdfServiceFactory,
            String dataType,
            String format) {
        RdfService<String> rdfService;
        return switch (format) {
            case RDF, TURTLE, N_TRIPLES -> {
                rdfService = rdfServiceFactory.getRdfService(dataType, format);
                yield rdfService.getProlog(firstBatch);
            }
            default -> throw new IllegalArgumentException(
                    String.format("Unsupported format %s", format));
        };
    }

    public String getClosingTag(String format) {
        return switch (format) {
            case RDF -> "</rdf:RDF>";
            case TURTLE, N_TRIPLES -> "";
            default -> throw new IllegalArgumentException(
                    String.format("Unsupported format %s", format));
        };
    }
}
