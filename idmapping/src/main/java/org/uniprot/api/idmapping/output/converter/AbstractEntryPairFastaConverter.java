package org.uniprot.api.idmapping.output.converter;

import java.io.IOException;
import java.io.OutputStream;

import org.springframework.http.MediaType;
import org.uniprot.api.idmapping.model.EntryPair;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;

/**
 * @author sahmad
 * @created 02/03/2021
 */
public abstract class AbstractEntryPairFastaConverter<T extends EntryPair<U>, U>
        extends AbstractEntityHttpMessageConverter<T> {
    protected AbstractEntryPairFastaConverter(
            MediaType mediaType, Class<T> messageConverterEntryClass) {
        super(mediaType, messageConverterEntryClass);
    }

    @Override
    protected void writeEntity(T entryPair, OutputStream outputStream) throws IOException {
        outputStream.write((toFasta(entryPair.getTo()) + "\n").getBytes());
    }

    protected abstract String toFasta(U entry);
}
