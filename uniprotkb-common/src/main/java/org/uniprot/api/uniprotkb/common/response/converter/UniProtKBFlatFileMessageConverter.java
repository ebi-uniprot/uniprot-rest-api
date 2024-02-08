package org.uniprot.api.uniprotkb.common.response.converter;

import java.io.IOException;
import java.io.OutputStream;

import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.core.flatfile.writer.impl.UniProtFlatfileWriter;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

public class UniProtKBFlatFileMessageConverter
        extends AbstractEntityHttpMessageConverter<UniProtKBEntry> {
    public UniProtKBFlatFileMessageConverter() {
        super(UniProtMediaType.FF_MEDIA_TYPE, UniProtKBEntry.class);
    }

    public UniProtKBFlatFileMessageConverter(Gatekeeper downloadGatekeeper) {
        super(UniProtMediaType.FF_MEDIA_TYPE, UniProtKBEntry.class, downloadGatekeeper);
    }

    @Override
    protected void writeEntity(UniProtKBEntry entity, OutputStream outputStream)
            throws IOException {
        if (entity.isActive()) {
            outputStream.write((UniProtFlatfileWriter.write(entity) + "\n").getBytes());
        }
    }
}
