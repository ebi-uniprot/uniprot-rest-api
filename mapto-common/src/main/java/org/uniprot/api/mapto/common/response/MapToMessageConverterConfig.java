package org.uniprot.api.mapto.common.response;

import java.util.List;
import java.util.Map;

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
import org.uniprot.core.parser.tsv.EntityValueMapper;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.returnfield.factory.ReturnFieldConfigFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;

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
                String idField =
                        ReturnFieldConfigFactory.getReturnFieldConfig(UniProtDataType.MAPTO_ID)
                                .getReturnFieldByName("id")
                                .getName();
                EntityValueMapper<MapToEntryId> entityValueMapper =
                        (entity, fieldNames) -> Map.of(idField, entity.getId());
                converters.add(
                        new XlsMessageConverter<>(
                                MapToEntryId.class,
                                ReturnFieldConfigFactory.getReturnFieldConfig(
                                        UniProtDataType.MAPTO_ID),
                                entityValueMapper,
                                downloadGatekeeper));
                converters.add(
                        new TsvMessageConverter<>(
                                MapToEntryId.class,
                                ReturnFieldConfigFactory.getReturnFieldConfig(
                                        UniProtDataType.MAPTO_ID),
                                entityValueMapper,
                                downloadGatekeeper));

                JsonMessageConverter<MapToEntryId> jsonMessageConverter =
                        new JsonMessageConverter<>(
                                new ObjectMapper(),
                                MapToEntryId.class,
                                ReturnFieldConfigFactory.getReturnFieldConfig(
                                        UniProtDataType.MAPTO_ID),
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
