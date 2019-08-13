package org.uniprot.api.uniprotkb.output.converter;

import java.io.IOException;
import java.io.OutputStream;

import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.core.parser.gff.uniprot.UniProtGffParser;
import org.uniprot.core.uniprot.UniProtEntry;

public class UniProtKBGffMessageConverter extends AbstractEntityHttpMessageConverter<UniProtEntry> {

    public UniProtKBGffMessageConverter() {
        super(UniProtMediaType.GFF_MEDIA_TYPE, UniProtEntry.class);
    }

    @Override
    protected void writeEntity(UniProtEntry entity, OutputStream outputStream) throws IOException {
        outputStream.write((UniProtGffParser.convert(entity) + "\n").getBytes());
    }
}
