package org.uniprot.api.rest.service;

import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.*;
import org.uniprot.store.datastore.common.StoreService;

public class RDFService<T> implements StoreService<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RDFService.class);
    private static final String QUERY_STR = "query";
    private static final String FORMAT_STR = "format";
    private static final String RDF_STR = "rdf";
    private static final String ID_COLON_STR = "id:";
    private static final String OR_DELIMITER_STR = " or ";
    private Class<T> clazz;
    private RestTemplate restTemplate;

    public RDFService(RestTemplate restTemplate, Class<T> clazz) {
        this.restTemplate = restTemplate;
        this.clazz = clazz;
    }

    @Override
    public List<T> getEntries(Iterable<String> accessions) {
        List<String> allAccessions = new ArrayList<>();
        accessions.forEach(acc -> allAccessions.add(acc));
        LOGGER.debug("RDF call for accessions : {}", allAccessions);
        T rdfXml = getEntriesByAccessions(allAccessions);
        return Arrays.asList(rdfXml);
    }

    @Override
    public String getStoreName() {
        return "RDF Store";
    }

    @Override
    public Optional<T> getEntry(String id) {
        return Optional.of(getEntriesByAccessions(Arrays.asList(id)));
    }

    private T getEntriesByAccessions(List<String> accessions) {
        // create query like id:P12345 or id:P54321 or ....
        String idQuery =
                accessions.stream()
                        .map(acc -> new StringBuilder(ID_COLON_STR).append(acc))
                        .collect(Collectors.joining(OR_DELIMITER_STR));

        DefaultUriBuilderFactory handler =
                (DefaultUriBuilderFactory) restTemplate.getUriTemplateHandler();

        UriBuilder uriBuilder =
                handler.builder().queryParam(QUERY_STR, idQuery).queryParam(FORMAT_STR, RDF_STR);

        T rdfResponse = restTemplate.getForObject(uriBuilder.build(), this.clazz);

        return rdfResponse;
    }
}
