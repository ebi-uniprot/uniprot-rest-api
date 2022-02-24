package org.uniprot.api.rest.output.converter;

import java.lang.reflect.Type;
import java.util.stream.Stream;

import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.rest.output.context.MessageConverterContext;

/**
 * Extends {@link AbstractUUWHttpMessageConverter} by implementing {@link
 * AbstractEntityHttpMessageConverter#entitiesToWrite(MessageConverterContext)} so that it returns
 * an entity ID (not actual entity) stream.
 *
 * <p>Created 31/10/18
 *
 * @author Edd
 */
public abstract class AbstractEntityIdHttpMessageConverter<C>
        extends AbstractUUWHttpMessageConverter<C, String> {

    AbstractEntityIdHttpMessageConverter(MediaType mediaType, Class<C> messageConverterEntryClass) {
        super(mediaType, messageConverterEntryClass, null);
    }

    AbstractEntityIdHttpMessageConverter(
            MediaType mediaType,
            Class<C> messageConverterEntryClass,
            Gatekeeper downloadGatekeeper) {
        super(mediaType, messageConverterEntryClass, downloadGatekeeper);
    }

    @Override
    public boolean canWrite(@Nullable Type type, Class<?> clazz, @Nullable MediaType mediaType) {
        return validMediaType(mediaType) && MessageConverterContext.class.isAssignableFrom(clazz);
    }

    @Override
    protected Stream<String> entitiesToWrite(MessageConverterContext<C> context) {
        return context.getEntityIds();
    }
}
