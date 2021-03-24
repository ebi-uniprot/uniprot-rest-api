package org.uniprot.api.unisave.output.converter;

import java.io.IOException;
import java.io.OutputStream;

import org.uniprot.api.rest.output.converter.JsonMessageConverter;
import org.uniprot.api.unisave.model.UniSaveEntry;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

/** @author eddturner */
public class UniSaveJsonMessageConverter extends JsonMessageConverter<UniSaveEntry> {

    public UniSaveJsonMessageConverter() {
        super(new ObjectMapper(), UniSaveEntry.class, null);
    }

    @Override
    public void writeEntity(UniSaveEntry entity, OutputStream outputStream) throws IOException {
        JsonGenerator generator = TL_JSON_GENERATOR.get();
        generator.writeObject(entity);
    }
}
