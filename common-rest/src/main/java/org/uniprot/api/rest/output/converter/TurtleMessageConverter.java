package org.uniprot.api.rest.output.converter;

import java.io.IOException;
import java.io.OutputStream;

import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.rest.output.UniProtMediaType;

public class TurtleMessageConverter extends AbstractEntityIdHttpMessageConverter<String> {
    public TurtleMessageConverter() {
        super(UniProtMediaType.TURTLE_MEDIA_TYPE, String.class);
    }

    public TurtleMessageConverter(Gatekeeper downloadGatekeeper) {
        super(UniProtMediaType.TURTLE_MEDIA_TYPE, String.class, downloadGatekeeper);
    }

    @Override
    protected void writeEntity(String entity, OutputStream outputStream) throws IOException {
        outputStream.write((entity).getBytes());
    }
}
