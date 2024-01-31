package org.uniprot.api.uniprotkb.common.response.converter;

import java.io.IOException;
import java.io.OutputStream;

import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.core.parser.fasta.uniprot.UniProtKBFastaParser;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

public class UniProtKBFastaMessageConverter
        extends AbstractEntityHttpMessageConverter<UniProtKBEntry> {
    public UniProtKBFastaMessageConverter() {
        super(UniProtMediaType.FASTA_MEDIA_TYPE, UniProtKBEntry.class);
    }

    public UniProtKBFastaMessageConverter(Gatekeeper downloadGatekeeper) {
        super(UniProtMediaType.FASTA_MEDIA_TYPE, UniProtKBEntry.class, downloadGatekeeper);
    }

    @Override
    protected void writeEntity(UniProtKBEntry entity, OutputStream outputStream)
            throws IOException {
        if (entity.isActive()) {
            outputStream.write((UniProtKBFastaParser.toFasta(entity) + "\n").getBytes());
        }
    }
}
