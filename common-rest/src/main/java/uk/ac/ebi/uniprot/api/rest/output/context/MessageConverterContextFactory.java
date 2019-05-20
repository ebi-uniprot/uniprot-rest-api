package uk.ac.ebi.uniprot.api.rest.output.context;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

/**
 * Created 10/09/18
 *
 * @author Edd
 */
public class MessageConverterContextFactory<T> {
    private final Map<Pair, MessageConverterContext<T>> converters = new HashMap<>();

    public void addMessageConverterContext(MessageConverterContext<T> converter) {
        Pair pair = Pair.builder().contentType(converter.getContentType()).resource(converter.getResource())
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
        UNIPROT, UNIREF, UNIPARC, PROTEOME, GENECENTRIC
    }
}
