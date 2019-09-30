package org.uniprot.api.uniprotkb.output.converter;

import java.io.IOException;
import java.io.OutputStream;

import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.core.parser.fasta.uniprot.UniprotFastaParser;
import org.uniprot.core.uniprot.UniProtEntry;

public class UniProtKBFastaMessageConverter
        extends AbstractEntityHttpMessageConverter<UniProtEntry> {
    public UniProtKBFastaMessageConverter() {
        super(UniProtMediaType.FASTA_MEDIA_TYPE, UniProtEntry.class);
    }

    @Override
    protected void writeEntity(UniProtEntry entity, OutputStream outputStream) throws IOException {
        outputStream.write((UniprotFastaParser.create(entity).toString() + "\n").getBytes());
    }
}
