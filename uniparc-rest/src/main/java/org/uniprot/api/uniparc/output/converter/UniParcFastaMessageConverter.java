package org.uniprot.api.uniparc.output.converter;

import java.io.IOException;
import java.io.OutputStream;

import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.core.parser.fasta.UniParcFastaParser;
import org.uniprot.core.uniparc.UniParcEntry;

/**
 * @author jluo
 * @date: 25 Jun 2019
 */
public class UniParcFastaMessageConverter extends AbstractEntityHttpMessageConverter<UniParcEntry> {
    public UniParcFastaMessageConverter() {
        super(UniProtMediaType.FASTA_MEDIA_TYPE, UniParcEntry.class);
    }

    @Override
    protected void writeEntity(UniParcEntry entity, OutputStream outputStream) throws IOException {
        outputStream.write((UniParcFastaParser.toFasta(entity) + "\n").getBytes());
    }
}
