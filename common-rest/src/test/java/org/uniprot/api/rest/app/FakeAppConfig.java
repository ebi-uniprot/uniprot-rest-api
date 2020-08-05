package org.uniprot.api.rest.app;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.output.converter.*;

/**
 * @author lgonzales
 * @since 29/07/2020
 */
@Configuration
public class FakeAppConfig {

    @Bean
    @Profile("use-fake-app")
    public MessageConverterContextFactory<String> messageConverterContextFactory() {
        MessageConverterContextFactory<String> contextFactory =
                new MessageConverterContextFactory<>();

        MessageConverterContext<String> converter =
                MessageConverterContext.<String>builder()
                        .resource(MessageConverterContextFactory.Resource.TAXONOMY)
                        .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                        .build();
        contextFactory.addMessageConverterContext(converter);
        return contextFactory;
    }

    @Bean
    @Profile("use-fake-app")
    public WebMvcConfigurer extendedMessageConverters() {
        return new WebMvcConfigurer() {
            @Override
            public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
                converters.add(new ErrorMessageConverter());
                converters.add(new ListMessageConverter());
            }
        };
    }
}
