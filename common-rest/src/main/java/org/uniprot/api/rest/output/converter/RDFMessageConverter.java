package org.uniprot.api.rest.output.converter;

import java.io.IOException;
import java.io.OutputStream;

import org.uniprot.api.rest.output.UniProtMediaType;

public class RDFMessageConverter extends AbstractEntityIdHttpMessageConverter<String> {
    public RDFMessageConverter() {
        super(UniProtMediaType.RDF_MEDIA_TYPE, String.class);
    }

    @Override
    protected void writeEntity(String entity, OutputStream outputStream) throws IOException {
        outputStream.write((entity + "\n").getBytes());
    }
}
