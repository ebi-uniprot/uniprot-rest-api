package org.uniprot.api.common.repository.stream.rdf;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.client.RestTemplate;
import org.uniprot.api.rest.service.RdfService;
import org.uniprot.api.rest.service.TagPositionProvider;

import lombok.Data;

public class RdfServiceFactory {
    private final Map<RdfServiceIdentifier, RdfService<String>> serviceMap = new HashMap<>();
    private final RestTemplate restTemplate;
    private final TagPositionProvider tagPositionProvider;

    public RdfServiceFactory(RestTemplate restTemplate, TagPositionProvider tagPositionProvider) {
        this.restTemplate = restTemplate;
        this.tagPositionProvider = tagPositionProvider;
    }

    public RdfService<String> getRdfService(String dataType, String format) {
        RdfServiceIdentifier rdfServiceIdentifier = new RdfServiceIdentifier(dataType, format);
        return serviceMap.computeIfAbsent(
                rdfServiceIdentifier,
                rSI ->
                        new RdfService<>(
                                tagPositionProvider, restTemplate, String.class, dataType, format));
    }

    @Data
    private static class RdfServiceIdentifier {
        private final String dataType;
        private final String format;
    }
}
