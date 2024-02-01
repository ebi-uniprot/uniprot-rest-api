package org.uniprot.api.idmapping.common.response.converter;

import java.io.IOException;
import java.io.OutputStream;

import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.common.repository.search.EntryPair;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.core.util.Utils;

/**
 * @author sahmad
 * @created 02/03/2021
 */
public abstract class AbstractEntryPairFastaConverter<T extends EntryPair<U>, U>
        extends AbstractEntityHttpMessageConverter<T> {
    protected AbstractEntryPairFastaConverter(Class<T> messageConverterEntryClass) {
        super(UniProtMediaType.FASTA_MEDIA_TYPE, messageConverterEntryClass);
    }

    protected AbstractEntryPairFastaConverter(
            Class<T> messageConverterEntryClass, Gatekeeper downloadGatekeeper) {
        super(UniProtMediaType.FASTA_MEDIA_TYPE, messageConverterEntryClass, downloadGatekeeper);
    }

    @Override
    protected void writeEntity(T entryPair, OutputStream outputStream) throws IOException {
        String fastaContent = toFasta(entryPair);
        if (Utils.notNullNotEmpty(fastaContent)) {
            outputStream.write((fastaContent + "\n").getBytes());
        }
    }

    protected abstract String toFasta(T entryPair);
}
