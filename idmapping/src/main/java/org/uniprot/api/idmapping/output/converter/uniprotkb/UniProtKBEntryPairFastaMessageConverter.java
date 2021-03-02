package org.uniprot.api.idmapping.output.converter.uniprotkb;

import org.uniprot.api.idmapping.model.UniProtKBEntryPair;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.core.parser.fasta.uniprot.UniProtKBFastaParser;

import java.io.IOException;
import java.io.OutputStream;

public class UniProtKBEntryPairFastaMessageConverter
        extends AbstractEntityHttpMessageConverter<UniProtKBEntryPair> {
    public UniProtKBEntryPairFastaMessageConverter() {
        super(UniProtMediaType.FASTA_MEDIA_TYPE, UniProtKBEntryPair.class);
    }

    @Override
    protected void writeEntity(UniProtKBEntryPair entity, OutputStream outputStream)
            throws IOException {
        outputStream.write((UniProtKBFastaParser.toFasta(entity.getTo()) + "\n").getBytes());
    }
}
