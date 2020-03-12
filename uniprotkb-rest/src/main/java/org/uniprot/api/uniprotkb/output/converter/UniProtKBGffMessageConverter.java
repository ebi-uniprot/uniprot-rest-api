package org.uniprot.api.uniprotkb.output.converter;

import java.io.IOException;
import java.io.OutputStream;

import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.core.parser.gff.uniprot.UniProtGffParser;
import org.uniprot.core.uniprotkb.UniProtkbEntry;

public class UniProtKBGffMessageConverter
        extends AbstractEntityHttpMessageConverter<UniProtkbEntry> {

    public UniProtKBGffMessageConverter() {
        super(UniProtMediaType.GFF_MEDIA_TYPE, UniProtkbEntry.class);
    }

    @Override
    protected void writeEntity(UniProtkbEntry entity, OutputStream outputStream)
            throws IOException {
        outputStream.write((UniProtGffParser.convert(entity) + "\n").getBytes());
    }
}
