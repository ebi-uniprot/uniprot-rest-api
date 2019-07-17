package uk.ac.ebi.uniprot.api.crossref.output;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContextFactory;
import uk.ac.ebi.uniprot.domain.crossref.CrossRefEntry;

import java.util.Arrays;

import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * @author sahmad
 */

@Configuration
public class CrossRefMessageConverterConfig {

    @Bean(name = "crossrefMessageConverterContextFactory")
    public MessageConverterContextFactory<CrossRefEntry> crossrefMessageConverterContextFactory() {
        MessageConverterContextFactory<CrossRefEntry> contextFactory = new MessageConverterContextFactory<>();

        Arrays.asList(
                context(APPLICATION_JSON)
        ).forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    private MessageConverterContext<CrossRefEntry> context(MediaType contentType) {
        return MessageConverterContext.<CrossRefEntry>builder()
                .resource(MessageConverterContextFactory.Resource.CROSSREF)
                .contentType(contentType)
                .build();
    }
}
