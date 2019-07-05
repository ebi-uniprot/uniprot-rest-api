package uk.ac.ebi.uniprot.api.literature.output;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContextFactory;
import uk.ac.ebi.uniprot.domain.literature.LiteratureEntry;

import static java.util.Arrays.asList;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * @author lgonzales
 * @since 2019-07-04
 */
@Configuration
public class LiteratureMessageConverterConfig {

    @Bean(name = "literatureMessageConverterContextFactory")
    public MessageConverterContextFactory<LiteratureEntry> literatureMessageConverterContextFactory() {
        MessageConverterContextFactory<LiteratureEntry> contextFactory = new MessageConverterContextFactory<>();

        asList(context(APPLICATION_JSON),
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
