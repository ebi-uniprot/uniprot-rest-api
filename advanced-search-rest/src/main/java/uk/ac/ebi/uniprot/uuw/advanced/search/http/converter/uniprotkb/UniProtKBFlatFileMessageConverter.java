package uk.ac.ebi.uniprot.uuw.advanced.search.http.converter.uniprotkb;

import uk.ac.ebi.kraken.ffwriter.line.impl.UniProtFlatfileWriter;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.UniProtMediaType;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.converter.AbstractEntityHttpMessageConverter;

import java.io.IOException;
import java.io.OutputStream;

public class UniProtKBFlatFileMessageConverter extends AbstractEntityHttpMessageConverter<UniProtEntry> {
    public UniProtKBFlatFileMessageConverter() {
        super(UniProtMediaType.FF_MEDIA_TYPE);
    }

    @Override
    protected void writeEntity(UniProtEntry entity, OutputStream outputStream) throws IOException {
        outputStream.write((UniProtFlatfileWriter.write(entity) + "\n").getBytes());
    }
}
