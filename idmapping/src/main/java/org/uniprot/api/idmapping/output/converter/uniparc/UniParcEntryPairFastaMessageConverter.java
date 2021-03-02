package org.uniprot.api.idmapping.output.converter.uniparc;

import org.uniprot.api.idmapping.model.UniParcEntryPair;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.core.parser.fasta.UniParcFastaParser;

import java.io.IOException;
import java.io.OutputStream;

public class UniParcEntryPairFastaMessageConverter extends AbstractEntityHttpMessageConverter<UniParcEntryPair> {
    public UniParcEntryPairFastaMessageConverter() {
        super(UniProtMediaType.FASTA_MEDIA_TYPE, UniParcEntryPair.class);
    }

    @Override
    protected void writeEntity(UniParcEntryPair entity, OutputStream outputStream) throws IOException {
        outputStream.write((UniParcFastaParser.toFasta(entity.getTo()) + "\n").getBytes());
    }
}
