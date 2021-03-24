package org.uniprot.api.uniparc.output.converter;

import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.api.uniparc.model.UniParcEntryWrapper;
import org.uniprot.core.parser.fasta.UniParcFastaParser;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author sahmad
 * @created 24/03/2021
 */
public class UniParcWrapperFastaMessageConverter extends AbstractEntityHttpMessageConverter<UniParcEntryWrapper> {
    public UniParcWrapperFastaMessageConverter() {
        super(UniProtMediaType.FASTA_MEDIA_TYPE, UniParcEntryWrapper.class);
    }
    @Override
    protected void writeEntity(UniParcEntryWrapper entity, OutputStream outputStream) throws IOException {
        outputStream.write((UniParcFastaParser.toFasta(entity.getEntry()) + "\n").getBytes());
    }
}
