package org.uniprot.api.common.repository.stream.rdf;


import org.springframework.stereotype.Component;
import org.uniprot.api.rest.service.RDFPrologs;

@Component
public class PrologProvider {
    public String getPreLog(String type, String format) {
        if ("diseases".equals(type) && "rdf".equals(format)) {
            return RDFPrologs.DISEASE_PROLOG;
        }
        return "";
    }

    public String getPostLog(String type, String format) {
        return "";
    }
}
