package org.uniprot.api.unisave.output.converter;

import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.converter.AbstractEntityHttpMessageConverter;
import org.uniprot.api.unisave.model.UniSaveEntry;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author eddturner
 */
public class UniSaveFlatFileMessageConverter extends AbstractEntityHttpMessageConverter<UniSaveEntry> {

    public UniSaveFlatFileMessageConverter() {
        super(UniProtMediaType.FF_MEDIA_TYPE, UniSaveEntry.class);
    }

    @Override
    protected void writeEntity(UniSaveEntry entity, OutputStream outputStream) throws IOException {
        outputStream.write(entity.getContent().getBytes());
    }
}
