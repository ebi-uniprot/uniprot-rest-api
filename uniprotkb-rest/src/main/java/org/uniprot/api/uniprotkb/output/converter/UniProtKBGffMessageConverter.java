package org.uniprot.api.uniprotkb.output.converter;

import java.io.IOException;
import java.io.OutputStream;

import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.core.parser.gff.uniprot.UniProtGffParser;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

public class UniProtKBGffMessageConverter
        extends AbstractEntityHttpMessageConverter<UniProtKBEntry> {

    public UniProtKBGffMessageConverter() {
        super(UniProtMediaType.GFF_MEDIA_TYPE, UniProtKBEntry.class);
    }

    @Override
    protected void writeEntity(UniProtKBEntry entity, OutputStream outputStream)
            throws IOException {
        if (entity.isActive()) {
            outputStream.write((UniProtGffParser.convert(entity) + "\n").getBytes());
        }
    }
}
