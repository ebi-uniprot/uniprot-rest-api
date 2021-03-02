package org.uniprot.api.idmapping.output.converter.uniref;

import org.uniprot.api.idmapping.model.UniRefEntryPair;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.core.parser.fasta.UniRefFastaParser;

import java.io.IOException;
import java.io.OutputStream;

public class UniRefEntryFastaMessageConverter extends AbstractEntityHttpMessageConverter<UniRefEntryPair> {

    public UniRefEntryFastaMessageConverter() {
        super(UniProtMediaType.FASTA_MEDIA_TYPE, UniRefEntryPair.class);
    }

    @Override
    protected void writeEntity(UniRefEntryPair entity, OutputStream outputStream) throws IOException {
        outputStream.write((UniRefFastaParser.toFasta(entity.getTo()) + "\n").getBytes());
    }
}
