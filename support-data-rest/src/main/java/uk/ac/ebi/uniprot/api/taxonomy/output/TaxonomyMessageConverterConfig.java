package uk.ac.ebi.uniprot.api.taxonomy.output;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContextFactory;
import uk.ac.ebi.uniprot.domain.taxonomy.TaxonomyEntry;

import static java.util.Arrays.asList;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 *
 * @author jluo
 * @date: 29 Apr 2019
 *
*/

@Configuration
public class TaxonomyMessageConverterConfig {

    @Bean(name = "taxonomyMessageConverterContextFactory")
    public MessageConverterContextFactory<TaxonomyEntry> taxonomyMessageConverterContextFactory() {
        MessageConverterContextFactory<TaxonomyEntry> contextFactory = new MessageConverterContextFactory<>();

        asList(context(UniProtMediaType.LIST_MEDIA_TYPE),
               context(APPLICATION_JSON),
               context(UniProtMediaType.TSV_MEDIA_TYPE),
               context(UniProtMediaType.XLS_MEDIA_TYPE))
                .forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    private MessageConverterContext<TaxonomyEntry> context(MediaType contentType) {
        return MessageConverterContext.<TaxonomyEntry>builder()
                .resource(MessageConverterContextFactory.Resource.TAXONOMY)
                .contentType(contentType)
                .build();
    }
}
