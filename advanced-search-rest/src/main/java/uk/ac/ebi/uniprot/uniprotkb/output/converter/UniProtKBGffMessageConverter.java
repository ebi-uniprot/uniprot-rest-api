package uk.ac.ebi.uniprot.uniprotkb.output.converter;

import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.EntryGffConverter;
import uk.ac.ebi.uniprot.rest.output.UniProtMediaType;
import uk.ac.ebi.uniprot.rest.output.converter.AbstractEntityHttpMessageConverter;

import java.io.IOException;
import java.io.OutputStream;

public class UniProtKBGffMessageConverter extends AbstractEntityHttpMessageConverter<UniProtEntry> {
    public UniProtKBGffMessageConverter() {
        super(UniProtMediaType.GFF_MEDIA_TYPE);
    }

    @Override
    protected void writeEntity(UniProtEntry entity, OutputStream outputStream) throws IOException {
        outputStream.write((EntryGffConverter.convert(entity) + "\n").getBytes());
    }
}
