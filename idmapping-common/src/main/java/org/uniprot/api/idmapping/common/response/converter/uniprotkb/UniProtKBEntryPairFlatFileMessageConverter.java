package org.uniprot.api.idmapping.common.response.converter.uniprotkb;

import java.io.IOException;
import java.io.OutputStream;

import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.idmapping.common.response.model.UniProtKBEntryPair;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.core.flatfile.writer.impl.UniProtFlatfileWriter;

/**
 * @author sahmad
 * @created 03/03/2021
 */
public class UniProtKBEntryPairFlatFileMessageConverter
        extends AbstractEntityHttpMessageConverter<UniProtKBEntryPair> {

    public UniProtKBEntryPairFlatFileMessageConverter() {
        super(UniProtMediaType.FF_MEDIA_TYPE, UniProtKBEntryPair.class);
    }

    public UniProtKBEntryPairFlatFileMessageConverter(Gatekeeper downloadGatekeeper) {
        super(UniProtMediaType.FF_MEDIA_TYPE, UniProtKBEntryPair.class, downloadGatekeeper);
    }

    @Override
    protected void writeEntity(UniProtKBEntryPair entryPair, OutputStream outputStream)
            throws IOException {
        if (entryPair.getTo().isActive()) {
            outputStream.write((UniProtFlatfileWriter.write(entryPair.getTo()) + "\n").getBytes());
        }
    }
}
