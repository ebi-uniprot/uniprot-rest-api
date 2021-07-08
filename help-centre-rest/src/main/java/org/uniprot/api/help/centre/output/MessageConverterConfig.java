package org.uniprot.api.help.centre.output;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.uniprot.api.help.centre.model.HelpCentreEntry;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.output.converter.*;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.config.ReturnFieldConfig;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Created 21/08/18
 *
 * @author Edd
 */
@Configuration
public class MessageConverterConfig {

    /*
     * Add to the supported message converters.
     * Add more message converters for additional response types.
     */
    @Bean
    public WebMvcConfigurer extendedMessageConverters() {
        ReturnFieldConfig returnConfig =
                ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.HELP);
        JsonMessageConverter<HelpCentreEntry> jsonMessageConverter =
                new JsonMessageConverter<>(new ObjectMapper(), HelpCentreEntry.class, returnConfig);

        return new WebMvcConfigurer() {
            @Override
            public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
                converters.add(jsonMessageConverter);
            }
        };
    }

    @Bean("helpCentreMessageConverterContextFactory")
    public MessageConverterContextFactory<HelpCentreEntry> messageConverterContextFactory() {
        MessageConverterContextFactory<HelpCentreEntry> contextFactory =
                new MessageConverterContextFactory<>();

        contextFactory.addMessageConverterContext(context(APPLICATION_JSON));

        return contextFactory;
    }

    private MessageConverterContext<HelpCentreEntry> context(MediaType contentType) {
        return MessageConverterContext.<HelpCentreEntry>builder()
                .resource(MessageConverterContextFactory.Resource.HELP)
                .contentType(contentType)
                .build();
    }
}
