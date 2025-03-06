package org.uniprot.api.uniparc.common.response.converter;

import java.io.IOException;
import java.io.OutputStream;

import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.converter.AbstractFastaMessageConverter;
import org.uniprot.core.parser.fasta.uniparc.UniParcFastaParser;
import org.uniprot.core.uniparc.UniParcEntryLight;

public class UniParcLightFastaMessageConverter
        extends AbstractFastaMessageConverter<UniParcEntryLight> {
    public UniParcLightFastaMessageConverter() {
        super(UniProtMediaType.FASTA_MEDIA_TYPE, UniParcEntryLight.class);
    }

    public UniParcLightFastaMessageConverter(Gatekeeper downloadGatekeeper) {
        super(UniProtMediaType.FASTA_MEDIA_TYPE, UniParcEntryLight.class, downloadGatekeeper);
    }

    @Override
    protected void writeEntity(UniParcEntryLight entity, OutputStream outputStream)
            throws IOException {
        String sequenceRange = getPassedSequenceRange(entity.getUniParcId());
        outputStream.write((UniParcFastaParser.toFasta(entity, sequenceRange) + "\n").getBytes());
    }
}
