package org.uniprot.api.rest.output.context;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;

import lombok.Builder;
import lombok.Data;

/**
 * Created 10/09/18
 *
 * @author Edd
 */
public class MessageConverterContextFactory<T> {
    private final Map<Pair, MessageConverterContext<T>> converters = new HashMap<>();

    public void addMessageConverterContext(MessageConverterContext<T> converter) {
        Pair pair =
                Pair.builder()
                        .contentType(converter.getContentType())
                        .resource(converter.getResource())
                        .build();
        this.converters.put(pair, converter);
    }

    public MessageConverterContext<T> get(Resource resource, MediaType contentType) {
        Pair pair = Pair.builder().contentType(contentType).resource(resource).build();

        MessageConverterContext<T> messageConverterContext = converters.get(pair);

        return messageConverterContext.asCopy();
    }

    @Data
    @Builder
    private static class Pair {
        private Resource resource;
        private MediaType contentType;
    }

    public enum Resource {
        UNIPROTKB,
        UNIPROTKB_PUBLICATION,
        UNIREF,
        UNISAVE,
        UNIPARC,
        PROTEOME,
        TAXONOMY,
        GENECENTRIC,
        IDMAPPING_PIR,
        KEYWORD,
        LITERATURE,
        DISEASE,
        CROSSREF,
        SUBCELLULAR_LOCATION,
        UNIPROTKB_INTERACTION,
        UNIRULE,
        HELP,
        ARBA
    }
}
