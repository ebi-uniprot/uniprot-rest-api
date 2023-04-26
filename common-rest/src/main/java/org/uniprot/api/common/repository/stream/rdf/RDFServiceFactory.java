package org.uniprot.api.common.repository.stream.rdf;

import lombok.Data;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.rest.service.RDFService;
import org.uniprot.api.rest.service.TagProvider;

import java.util.HashMap;
import java.util.Map;

public class RDFServiceFactory {
    private final Map<RDFServiceIdentifier, RDFService<String>> serviceMap = new HashMap<>();
    private final RestTemplate restTemplate;
    private final TagProvider tagProvider;

    public RDFServiceFactory(RestTemplate restTemplate, TagProvider tagProvider) {
        this.restTemplate = restTemplate;
        this.tagProvider = tagProvider;
    }

    public RDFService<String> getRdfService(String type, String format) {
        RDFServiceIdentifier rdfServiceIdentifier = new RDFServiceIdentifier(type, format);
        return serviceMap.computeIfAbsent(rdfServiceIdentifier, rSI -> new RDFService<>(tagProvider, restTemplate, String.class, type, format));
    }


    @Data
    private static class RDFServiceIdentifier {
        private final String type;
        private final String format;
    }

}
