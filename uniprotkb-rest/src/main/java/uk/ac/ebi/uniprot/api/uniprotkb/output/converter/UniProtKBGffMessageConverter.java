package uk.ac.ebi.uniprot.api.uniprotkb.output.converter;

import uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType;
import uk.ac.ebi.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import uk.ac.ebi.uniprot.domain.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.parser.gff.uniprot.UniProtGffParser;

import java.io.IOException;
import java.io.OutputStream;

public class UniProtKBGffMessageConverter extends AbstractEntityHttpMessageConverter<UniProtEntry> {

    public UniProtKBGffMessageConverter() {
        super(UniProtMediaType.GFF_MEDIA_TYPE, UniProtEntry.class);
    }

    @Override
    protected void writeEntity(UniProtEntry entity, OutputStream outputStream) throws IOException {
        outputStream.write((UniProtGffParser.convert(entity) + "\n").getBytes());
    }
}
