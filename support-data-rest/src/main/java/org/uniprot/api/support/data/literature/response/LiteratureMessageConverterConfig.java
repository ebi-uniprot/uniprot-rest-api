package org.uniprot.api.support.data.literature.response;

import static java.util.Arrays.asList;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.literature.LiteratureEntry;

/**
 * @author lgonzales
 * @since 2019-07-04
 */
@Configuration
public class LiteratureMessageConverterConfig {

    @Bean(name = "literatureMessageConverterContextFactory")
    public MessageConverterContextFactory<LiteratureEntry>
            literatureMessageConverterContextFactory() {
        MessageConverterContextFactory<LiteratureEntry> contextFactory =
                new MessageConverterContextFactory<>();

        asList(
                        context(APPLICATION_JSON),
                        context(UniProtMediaType.LIST_MEDIA_TYPE),
                        context(UniProtMediaType.TSV_MEDIA_TYPE),
                        context(UniProtMediaType.XLS_MEDIA_TYPE))
                .forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    private MessageConverterContext<LiteratureEntry> context(MediaType contentType) {
        return MessageConverterContext.<LiteratureEntry>builder()
                .resource(MessageConverterContextFactory.Resource.LITERATURE)
                .contentType(contentType)
                .build();
    }
}
