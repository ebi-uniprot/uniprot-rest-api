package uk.ac.ebi.uniprot.api.keyword.output;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContextFactory;
import uk.ac.ebi.uniprot.cv.keyword.KeywordEntry;

import static java.util.Arrays.asList;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * @author jluo
 * @date: 29 Apr 2019
 */

@Configuration
public class KeywordMessageConverterConfig {

    @Bean(name = "keywordMessageConverterContextFactory")
    public MessageConverterContextFactory<KeywordEntry> keywordMessageConverterContextFactory() {
        MessageConverterContextFactory<KeywordEntry> contextFactory = new MessageConverterContextFactory<>();

        asList(context(UniProtMediaType.LIST_MEDIA_TYPE),
                context(APPLICATION_JSON),
                context(UniProtMediaType.TSV_MEDIA_TYPE),
                context(UniProtMediaType.XLS_MEDIA_TYPE))
                .forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    private MessageConverterContext<KeywordEntry> context(MediaType contentType) {
        return MessageConverterContext.<KeywordEntry>builder()
                .resource(MessageConverterContextFactory.Resource.KEYWORD)
                .contentType(contentType)
                .build();
    }
}
