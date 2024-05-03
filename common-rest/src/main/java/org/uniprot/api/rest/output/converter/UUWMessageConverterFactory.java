package org.uniprot.api.rest.output.converter;

import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.uniprot.api.rest.output.context.MessageConverterContext;

import java.lang.reflect.Type;
import java.util.List;

@Component
public class UUWMessageConverterFactory {
    private final List<HttpMessageConverter<?>> messageConverters;

    public UUWMessageConverterFactory(List<HttpMessageConverter<?>> messageConverters) {
        this.messageConverters = messageConverters;
    }

    public AbstractUUWHttpMessageConverter<?, ?> getOutputWriter(
            MediaType contentType, Type type) {
        return messageConverters.stream()
                .filter(AbstractUUWHttpMessageConverter.class::isInstance)
                .filter(
                        converter ->
                                ((AbstractUUWHttpMessageConverter<?, ?>) converter)
                                        .canWrite(type, MessageConverterContext.class, contentType))
                .map(converter -> (AbstractUUWHttpMessageConverter<?, ?>) converter)
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException("Unable to find Message converter"));
    }
}
