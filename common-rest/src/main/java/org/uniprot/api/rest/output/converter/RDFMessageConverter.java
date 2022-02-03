package org.uniprot.api.rest.output.converter;

import java.io.IOException;
import java.io.OutputStream;

import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.rest.output.UniProtMediaType;

public class RDFMessageConverter extends AbstractEntityIdHttpMessageConverter<String> {
    public RDFMessageConverter() {
        super(UniProtMediaType.RDF_MEDIA_TYPE, String.class);
    }

    public RDFMessageConverter(Gatekeeper downloadGatekeeper) {
        super(UniProtMediaType.RDF_MEDIA_TYPE, String.class, downloadGatekeeper);
    }

    @Override
    protected void writeEntity(String entity, OutputStream outputStream) throws IOException {
        outputStream.write((entity + "\n").getBytes());
    }
}
