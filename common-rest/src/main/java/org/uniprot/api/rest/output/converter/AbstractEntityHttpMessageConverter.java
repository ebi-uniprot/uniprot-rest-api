package org.uniprot.api.rest.output.converter;

import java.util.stream.Stream;

import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;

/**
 * Extends {@link AbstractUUWHttpMessageConverter} by implementing {@link
 * AbstractEntityHttpMessageConverter#entitiesToWrite(MessageConverterContext)} so that it returns
 * the actual entity (not entity ID) stream.
 *
 * <p>Created 31/10/18
 *
 * @author Edd
 */
public abstract class AbstractEntityHttpMessageConverter<C>
        extends AbstractUUWHttpMessageConverter<C, C> {

    public AbstractEntityHttpMessageConverter(
            MediaType mediaType, Class<C> messageConverterEntryClass) {
        super(mediaType, messageConverterEntryClass);
    }

    @Override
    protected Stream<C> entitiesToWrite(MessageConverterContext<C> context) {
        return context.getEntities();
    }
}
