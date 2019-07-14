package uk.ac.ebi.uniprot.api.disease.response;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContextFactory;
import uk.ac.ebi.uniprot.cv.disease.Disease;

import java.util.Arrays;

import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * @author sahmad
 */

@Configuration
public class DiseaseMessageConverterConfig {

    @Bean(name = "diseaseMessageConverterContextFactory")
    public MessageConverterContextFactory<Disease> diseaseMessageConverterContextFactory() {
        MessageConverterContextFactory<Disease> contextFactory = new MessageConverterContextFactory<>();

        Arrays.asList(
                context(UniProtMediaType.LIST_MEDIA_TYPE),
                context(APPLICATION_JSON),
                context(UniProtMediaType.TSV_MEDIA_TYPE),
                context(UniProtMediaType.XLS_MEDIA_TYPE)
              ).forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    private MessageConverterContext<Disease> context(MediaType contentType) {
        return MessageConverterContext.<Disease>builder()
                .resource(MessageConverterContextFactory.Resource.DISEASE)
                .contentType(contentType)
                .build();
    }
}
