package org.uniprot.api.mapto.common.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.mapto.common.model.MapToEntryId;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.output.converter.*;
import org.uniprot.core.parser.tsv.unirule.UniRuleEntryValueMapper;

import java.util.List;

@Configuration
@Getter
@Setter
public class MapToMessageConverterConfig {
    @Bean
    public WebMvcConfigurer extendedMessageConverters(Gatekeeper downloadGatekeeper) {
        return new WebMvcConfigurer() {
            @Override
            public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
                converters.add(new ErrorMessageConverter());
                converters.add(new ErrorMessageXlsConverter());
                converters.add(new ListMessageConverter(downloadGatekeeper));
                converters.add(
                        new XlsMessageConverter<>(
                                MapToEntryId.class,
                                null,
                                null,
                                downloadGatekeeper));
                converters.add(
                        new TsvMessageConverter<>(
                                MapToEntryId.class,
                                null,
                                null,
                                downloadGatekeeper));

                JsonMessageConverter<MapToEntryId> jsonMessageConverter =
                        new JsonMessageConverter<>(
                                new ObjectMapper(),
                                MapToEntryId.class,
                                null,
                                downloadGatekeeper);
                converters.add(0, jsonMessageConverter);
            }
        };
    }

    @Bean
    public MessageConverterContextFactory<MapToEntryId>
    mapToEntryIdMessageConverterContextFactory() {
        MessageConverterContextFactory<MapToEntryId> contextFactory =
                new MessageConverterContextFactory<>();
        List.of(
                        mapToEntryIdContext(MediaType.APPLICATION_JSON),
                        mapToEntryIdContext(UniProtMediaType.LIST_MEDIA_TYPE),
                        mapToEntryIdContext(UniProtMediaType.TSV_MEDIA_TYPE),
                        mapToEntryIdContext(UniProtMediaType.XLS_MEDIA_TYPE))
                .forEach(contextFactory::addMessageConverterContext);
        return contextFactory;
    }

    private MessageConverterContext<MapToEntryId> mapToEntryIdContext(MediaType contentType) {
        return MessageConverterContext.<MapToEntryId>builder()
                .resource(MessageConverterContextFactory.Resource.MAPTO_ENTRY_ID)
                .contentType(contentType)
                .build();
    }
}
