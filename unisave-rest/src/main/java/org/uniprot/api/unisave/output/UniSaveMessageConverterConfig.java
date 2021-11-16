package org.uniprot.api.unisave.output;

import static java.util.Arrays.asList;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.uniprot.api.rest.output.UniProtMediaType.*;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.output.converter.ErrorMessageConverter;
import org.uniprot.api.unisave.model.UniSaveEntry;
import org.uniprot.api.unisave.output.converter.UniSaveFastaMessageConverter;
import org.uniprot.api.unisave.output.converter.UniSaveFlatFileMessageConverter;
import org.uniprot.api.unisave.output.converter.UniSaveJsonMessageConverter;
import org.uniprot.api.unisave.output.converter.UniSaveTSVMessageConverter;

/** @author Edd */
@Configuration
@Getter
@Setter
public class UniSaveMessageConverterConfig {
    @Bean
    public ThreadPoolTaskExecutor configurableTaskExecutor() {
        return new ThreadPoolTaskExecutor();
    }

    @Bean
    public WebMvcConfigurer extendedMessageConverters() {
        return new WebMvcConfigurer() {
            @Override
            public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
                converters.add(new ErrorMessageConverter());
                converters.add(new UniSaveFastaMessageConverter());
                converters.add(new UniSaveFlatFileMessageConverter());
                converters.add(new UniSaveTSVMessageConverter());
                converters.add(0, new UniSaveJsonMessageConverter());
            }
        };
    }

    @Bean
    public MessageConverterContextFactory<UniSaveEntry> uniSaveMessageConverterContextFactory() {
        MessageConverterContextFactory<UniSaveEntry> contextFactory =
                new MessageConverterContextFactory<>();

        asList(
                        unisaveContext(APPLICATION_JSON),
                        unisaveContext(FASTA_MEDIA_TYPE),
                        unisaveContext(FF_MEDIA_TYPE),
                        unisaveContext(TSV_MEDIA_TYPE))
                .forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    private MessageConverterContext<UniSaveEntry> unisaveContext(MediaType contentType) {
        return MessageConverterContext.<UniSaveEntry>builder()
                .resource(MessageConverterContextFactory.Resource.UNISAVE)
                .contentType(contentType)
                .build();
    }
}
