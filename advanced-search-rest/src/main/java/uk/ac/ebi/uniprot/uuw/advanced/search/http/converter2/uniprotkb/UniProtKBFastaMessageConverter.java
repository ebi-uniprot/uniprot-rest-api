package uk.ac.ebi.uniprot.uuw.advanced.search.http.converter2.uniprotkb;

import uk.ac.ebi.kraken.ffwriter.UniprotFasta;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.MessageConverterContext;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.UniProtMediaType;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.converter2.AbstractUUWHttpMessageConverter;

import java.io.IOException;
import java.io.OutputStream;

public class UniProtKBFastaMessageConverter extends AbstractUUWHttpMessageConverter<MessageConverterContext, UniProtEntry> {
    public UniProtKBFastaMessageConverter() {
        super(UniProtMediaType.FASTA_MEDIA_TYPE);
    }

    @Override
    protected void writeEntity(UniProtEntry entity, OutputStream outputStream) throws IOException {
        outputStream.write((UniprotFasta.create(entity).toString() + "\n").getBytes());
    }
}
