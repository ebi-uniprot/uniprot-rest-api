package org.uniprot.api.common.repository.stream.rdf;

import lombok.Data;
import org.springframework.web.client.RestTemplate;
import org.uniprot.api.rest.service.RDFService;
import org.uniprot.api.rest.service.TagPositionProvider;

import java.util.HashMap;
import java.util.Map;

public class RDFServiceFactory {
    private final Map<RDFServiceIdentifier, RDFService<String>> serviceMap = new HashMap<>();
    private final RestTemplate restTemplate;
    private final TagPositionProvider tagPositionProvider;

    public RDFServiceFactory(RestTemplate restTemplate, TagPositionProvider tagPositionProvider) {
        this.restTemplate = restTemplate;
        this.tagPositionProvider = tagPositionProvider;
    }

    public RDFService<String> getRdfService(String dataType, String format) {
        RDFServiceIdentifier rdfServiceIdentifier = new RDFServiceIdentifier(dataType, format);
        return serviceMap.computeIfAbsent(rdfServiceIdentifier, rSI -> new RDFService<>(tagPositionProvider, restTemplate, String.class, dataType, format));
    }


    @Data
    private static class RDFServiceIdentifier {
        private final String dataType;
        private final String format;
    }

}
