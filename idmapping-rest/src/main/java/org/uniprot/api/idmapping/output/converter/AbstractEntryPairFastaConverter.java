package org.uniprot.api.idmapping.output.converter;

import static org.uniprot.api.rest.output.UniProtMediaType.FASTA_MEDIA_TYPE;

import java.io.IOException;
import java.io.OutputStream;

import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.idmapping.model.EntryPair;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;

/**
 * @author sahmad
 * @created 02/03/2021
 */
public abstract class AbstractEntryPairFastaConverter<T extends EntryPair<U>, U>
        extends AbstractEntityHttpMessageConverter<T> {
    protected AbstractEntryPairFastaConverter(Class<T> messageConverterEntryClass) {
        super(FASTA_MEDIA_TYPE, messageConverterEntryClass);
    }

    protected AbstractEntryPairFastaConverter(
            Class<T> messageConverterEntryClass, Gatekeeper downloadGatekeeper) {
        super(FASTA_MEDIA_TYPE, messageConverterEntryClass, downloadGatekeeper);
    }

    @Override
    protected void writeEntity(T entryPair, OutputStream outputStream) throws IOException {
        outputStream.write((toFasta(entryPair) + "\n").getBytes());
    }

    protected abstract String toFasta(T entryPair);
}
