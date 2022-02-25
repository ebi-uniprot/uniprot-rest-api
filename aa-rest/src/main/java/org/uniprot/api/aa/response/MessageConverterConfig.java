package org.uniprot.api.aa.response;

import static java.util.Arrays.asList;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.output.converter.ErrorMessageConverter;
import org.uniprot.api.rest.output.converter.ErrorMessageXMLConverter;
import org.uniprot.api.rest.output.converter.JsonMessageConverter;
import org.uniprot.api.rest.output.converter.ListMessageConverter;
import org.uniprot.api.rest.output.converter.TsvMessageConverter;
import org.uniprot.api.rest.output.converter.XlsMessageConverter;
import org.uniprot.core.json.parser.unirule.UniRuleJsonConfig;
import org.uniprot.core.parser.tsv.unirule.UniRuleEntryValueMapper;
import org.uniprot.core.unirule.UniRuleEntry;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;

/**
 * @author sahmad
 * @created 11/11/2020
 */
@Configuration
public class MessageConverterConfig {
    @Bean(name = "uniRuleMessageConverterContextFactory")
    public MessageConverterContextFactory<UniRuleEntry> uniRuleMessageConverterContextFactory() {
        MessageConverterContextFactory<UniRuleEntry> contextFactory =
                new MessageConverterContextFactory<>();

        asList(
                        uniRuleContext(APPLICATION_JSON),
                        uniRuleContext(UniProtMediaType.LIST_MEDIA_TYPE),
                        uniRuleContext(UniProtMediaType.TSV_MEDIA_TYPE),
                        uniRuleContext(UniProtMediaType.XLS_MEDIA_TYPE))
                .forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    @Bean(name = "arbaMessageConverterContextFactory")
    public MessageConverterContextFactory<UniRuleEntry> arbaMessageConverterContextFactory() {
        MessageConverterContextFactory<UniRuleEntry> contextFactory =
                new MessageConverterContextFactory<>();
        asList(arbaContext(APPLICATION_JSON), arbaContext(UniProtMediaType.LIST_MEDIA_TYPE))
                .forEach(contextFactory::addMessageConverterContext);

        return contextFactory;
    }

    @Bean
    public WebMvcConfigurer extendedMessageConverters() {
        return new WebMvcConfigurer() {
            @Override
            public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
                var returnConfig =
                        ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.UNIRULE);

                converters.add(new ErrorMessageConverter());
                converters.add(new ErrorMessageXMLConverter()); // to handle xml error messages
                converters.add(new ListMessageConverter());
                converters.add(
                        new XlsMessageConverter<>(
                                UniRuleEntry.class, returnConfig, new UniRuleEntryValueMapper()));
                converters.add(
                        new TsvMessageConverter<>(
                                UniRuleEntry.class, returnConfig, new UniRuleEntryValueMapper()));

                JsonMessageConverter<UniRuleEntry> uniRuleJsonMessageConverter =
                        new JsonMessageConverter<>(
                                UniRuleJsonConfig.getInstance().getSimpleObjectMapper(),
                                UniRuleEntry.class,
                                returnConfig);
                converters.add(0, uniRuleJsonMessageConverter);
            }
        };
    }

    private MessageConverterContext<UniRuleEntry> uniRuleContext(MediaType contentType) {
        return MessageConverterContext.<UniRuleEntry>builder()
                .resource(MessageConverterContextFactory.Resource.UNIRULE)
                .contentType(contentType)
                .build();
    }

    private MessageConverterContext<UniRuleEntry> arbaContext(MediaType contentType) {
        return MessageConverterContext.<UniRuleEntry>builder()
                .resource(MessageConverterContextFactory.Resource.ARBA)
                .contentType(contentType)
                .build();
    }
}
