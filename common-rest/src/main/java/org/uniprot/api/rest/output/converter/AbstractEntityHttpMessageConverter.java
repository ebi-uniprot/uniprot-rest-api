package org.uniprot.api.rest.output.converter;

import java.util.stream.Stream;

import org.springframework.http.MediaType;
import org.uniprot.api.common.concurrency.Gatekeeper;
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
        super(mediaType, messageConverterEntryClass, null);
    }

    public AbstractEntityHttpMessageConverter(
            MediaType mediaType, Class<C> messageConverterEntryClass, Gatekeeper downloadGatekeeper) {
        super(mediaType, messageConverterEntryClass, downloadGatekeeper);
    }

    @Override
    protected Stream<C> entitiesToWrite(MessageConverterContext<C> context) {
        return context.getEntities();
    }
}
