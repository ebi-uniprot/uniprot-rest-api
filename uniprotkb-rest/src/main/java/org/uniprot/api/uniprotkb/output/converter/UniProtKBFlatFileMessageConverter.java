package org.uniprot.api.uniprotkb.output.converter;

import java.io.IOException;
import java.io.OutputStream;

import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.core.flatfile.writer.impl.UniProtFlatfileWriter;
import org.uniprot.core.uniprot.UniProtEntry;

public class UniProtKBFlatFileMessageConverter
        extends AbstractEntityHttpMessageConverter<UniProtEntry> {
    public UniProtKBFlatFileMessageConverter() {
        super(UniProtMediaType.FF_MEDIA_TYPE, UniProtEntry.class);
    }

    @Override
    protected void writeEntity(UniProtEntry entity, OutputStream outputStream) throws IOException {
        outputStream.write((UniProtFlatfileWriter.write(entity) + "\n").getBytes());
    }
}
