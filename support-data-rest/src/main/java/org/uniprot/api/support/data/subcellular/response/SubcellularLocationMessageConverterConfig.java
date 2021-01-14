package org.uniprot.api.support.data.subcellular.response;

import static java.util.Arrays.asList;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.cv.subcell.SubcellularLocationEntry;

/**
 * @author lgonzales
 * @since 2019-07-19
 */
@Configuration
public class SubcellularLocationMessageConverterConfig {

    @Bean(name = "subcellularLocationMessageConverterContextFactory")
    public MessageConverterContextFactory<SubcellularLocationEntry>
            subcellularLocationMessageConverterContextFactory() {
        MessageConverterContextFactory<SubcellularLocationEntry> contextFactory =
                new MessageConverterContextFactory<>();

        asList(
                        context(UniProtMediaType.LIST_MEDIA_TYPE),
                        context(APPLICATION_JSON),
                        context(UniProtMediaType.TSV_MEDIA_TYPE),
                        context(UniProtMediaType.XLS_MEDIA_TYPE),
                        context(UniProtMediaType.OBO_MEDIA_TYPE))
                .forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    private MessageConverterContext<SubcellularLocationEntry> context(MediaType contentType) {
        return MessageConverterContext.<SubcellularLocationEntry>builder()
                .resource(MessageConverterContextFactory.Resource.SUBCELLULAR_LOCATION)
                .contentType(contentType)
                .build();
    }
}
