package uk.ac.ebi.uniprot.uuw.advanced.search.http.context;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created 10/09/18
 *
 * @author Edd
 */
public class MessageConverterContextFactory {
    private final Map<Pair, MessageConverterContext> converters = new HashMap<>();

    public void addMessageConverterContexts(List<MessageConverterContext> converters) {
        converters.forEach(converter -> {
            Pair pair = Pair.builder().contentType(converter.getContentType()).resource(converter.getResource())
                    .build();
            this.converters.put(pair, converter);
        });
    }

    public MessageConverterContext get(Resource resource, MediaType contentType) {
        Pair pair = Pair.builder().contentType(contentType).resource(resource).build();

        MessageConverterContext messageConverterContext = converters.get(pair);

        return messageConverterContext.asCopy();
    }

    @Data
    @Builder
    private static class Pair {
        private Resource resource;
        private MediaType contentType;
    }

    public enum Resource {
        UNIPROT, UNIREF, UNIPARC
    }
}
