package org.uniprot.api.support.data.disease.response;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.core.cv.disease.DiseaseEntry;

import java.util.Arrays;

import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * @author sahmad
 */
@Configuration
public class DiseaseMessageConverterConfig {

    @Bean(name = "diseaseMessageConverterContextFactory")
    public MessageConverterContextFactory<DiseaseEntry> diseaseMessageConverterContextFactory() {
        MessageConverterContextFactory<DiseaseEntry> contextFactory =
                new MessageConverterContextFactory<>();

        Arrays.asList(
                        context(UniProtMediaType.LIST_MEDIA_TYPE),
                        context(APPLICATION_JSON),
                        context(UniProtMediaType.TSV_MEDIA_TYPE),
                        context(UniProtMediaType.XLS_MEDIA_TYPE),
                        context(UniProtMediaType.OBO_MEDIA_TYPE),
                        context(UniProtMediaType.RDF_MEDIA_TYPE),
                        context(UniProtMediaType.TTL_MEDIA_TYPE),
                        context(UniProtMediaType.NT_MEDIA_TYPE))
                .forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    private MessageConverterContext<DiseaseEntry> context(MediaType contentType) {
        return MessageConverterContext.<DiseaseEntry>builder()
                .resource(MessageConverterContextFactory.Resource.DISEASE)
                .contentType(contentType)
                .build();
    }
}
