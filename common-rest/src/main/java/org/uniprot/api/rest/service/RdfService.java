package org.uniprot.api.rest.service;

import java.net.URI;
import java.util.*;

import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.uniprot.store.datastore.common.StoreService;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RdfService<T> implements StoreService<T> {
    private final TagPositionProvider tagPositionProvider;
    private final RestTemplate restTemplate;
    private final Class<T> clazz;
    @Getter private final String dataType;
    @Getter private final String format;

    public RdfService(
            TagPositionProvider tagPositionProvider,
            RestTemplate restTemplate,
            Class<T> clazz,
            String dataType,
            String format) {
        this.tagPositionProvider = tagPositionProvider;
        this.restTemplate = restTemplate;
        this.clazz = clazz;
        this.dataType = dataType;
        this.format = format;
    }

    @Override
    public List<T> getEntries(Iterable<String> ids) {
        List<String> allAccessions = new ArrayList<>();
        ids.forEach(allAccessions::add);
        log.debug("RDF call for accessions : {}", allAccessions);
        T rdfXML = getEntriesByAccessions(allAccessions, dataType, format);

        if (Objects.nonNull(rdfXML)) {
            T rdfResponse = convertRdfForStreaming(rdfXML);
            return Collections.singletonList(rdfResponse);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public String getStoreName() {
        return String.format("%s %s Store", dataType, format);
    }

    @Override
    public Optional<T> getEntry(String id) {
        return Optional.ofNullable(
                getEntriesByAccessions(Collections.singletonList(id), dataType, format));
    }

    private T getEntriesByAccessions(List<String> accessions, String dataType, String format) {
        String commaSeparatedIds = String.join(",", accessions);
        DefaultUriBuilderFactory handler =
                (DefaultUriBuilderFactory) restTemplate.getUriTemplateHandler();
        URI requestUri = handler.builder().build(dataType, format, commaSeparatedIds);

        return restTemplate.getForObject(requestUri, this.clazz);
    }

    private T convertRdfForStreaming(T body) {
        T rdfResponse = body;
        if (this.clazz == String.class) {
            String bodyString = (String) body;
            int startingPosition = tagPositionProvider.getStartingPosition(bodyString, format);
            int indexOfCloseTag = tagPositionProvider.getEndingPosition(bodyString, format);
            rdfResponse = (T) bodyString.substring(startingPosition, indexOfCloseTag);
        }
        return rdfResponse;
    }
}
