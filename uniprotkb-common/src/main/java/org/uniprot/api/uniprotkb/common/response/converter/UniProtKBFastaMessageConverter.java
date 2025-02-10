package org.uniprot.api.uniprotkb.common.response.converter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.converter.AbstractFastaMessageConverter;
import org.uniprot.core.fasta.UniProtKBFasta;
import org.uniprot.core.parser.fasta.uniprot.UniProtKBFastaParser;
import org.uniprot.core.uniprotkb.UniProtKBEntry;

public class UniProtKBFastaMessageConverter extends AbstractFastaMessageConverter<UniProtKBEntry> {

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
            String sequenceRange = getPassedSequenceRange(entity.getPrimaryAccession().getValue());
            if (Objects.isNull(sequenceRange)) {
                outputStream.write((UniProtKBFastaParser.toFastaString(entity) + "\n").getBytes());
            } else {
                UniProtKBFasta uniProtKBFasta =
                        UniProtKBFastaParser.toUniProtKBFasta(entity, sequenceRange);
                outputStream.write(
                        (UniProtKBFastaParser.toFastaString(uniProtKBFasta) + "\n").getBytes());
            }
        }
    }
}
