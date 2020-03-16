package org.uniprot.api.uniprotkb.output.converter;

import java.io.IOException;
import java.io.OutputStream;

import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.core.parser.fasta.uniprot.UniprotkbFastaParser;
import org.uniprot.core.uniprotkb.UniProtkbEntry;

public class UniProtKBFastaMessageConverter
        extends AbstractEntityHttpMessageConverter<UniProtkbEntry> {
    public UniProtKBFastaMessageConverter() {
        super(UniProtMediaType.FASTA_MEDIA_TYPE, UniProtkbEntry.class);
    }

    @Override
    protected void writeEntity(UniProtkbEntry entity, OutputStream outputStream)
            throws IOException {
        outputStream.write((UniprotkbFastaParser.create(entity).toString() + "\n").getBytes());
    }
}
