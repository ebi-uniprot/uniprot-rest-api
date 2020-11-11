package org.uniprot.api.unirule.response;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.unirule.UniRuleEntry;

import static java.util.Arrays.asList;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * @author sahmad
 * @created 11/11/2020
 */
@Configuration
public class UniRuleMessageConverterConfig {

    @Bean(name = "uniRuleMessageConverterContextFactory")
    public MessageConverterContextFactory<UniRuleEntry>
    uniRuleMessageConverterContextFactory() {
        MessageConverterContextFactory<UniRuleEntry> contextFactory =
                new MessageConverterContextFactory<>();

        asList(
                context(APPLICATION_JSON),
                context(UniProtMediaType.LIST_MEDIA_TYPE),
                context(UniProtMediaType.TSV_MEDIA_TYPE),
                context(UniProtMediaType.XLS_MEDIA_TYPE))
                .forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    private MessageConverterContext<UniRuleEntry> context(MediaType contentType) {
        return MessageConverterContext.<UniRuleEntry>builder()
                .resource(MessageConverterContextFactory.Resource.UNIRULE)
                .contentType(contentType)
                .build();
    }
}
