package uk.ac.ebi.uniprot.api.rest.output.converter;

import org.springframework.http.MediaType;

import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;

import java.util.stream.Stream;

/**
 * Extends {@link AbstractUUWHttpMessageConverter} by implementing {@link AbstractEntityHttpMessageConverter#entitiesToWrite(MessageConverterContext)}
 * so that it returns an entity ID (not actual entity) stream.
 *
 * Created 31/10/18
 *
 * @author Edd
 */
public abstract class AbstractEntityIdHttpMessageConverter<C> extends AbstractUUWHttpMessageConverter<C, String> {

    AbstractEntityIdHttpMessageConverter(MediaType mediaType) {
        super(mediaType);
    }

    @Override
    protected Stream<String> entitiesToWrite(MessageConverterContext<C> context) {
        return context.getEntityIds();
    }
}
