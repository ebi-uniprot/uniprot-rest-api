package uk.ac.ebi.uniprot.api.rest.output.converter;


import java.io.IOException;
import java.io.OutputStream;

import uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType;

public class ListMessageConverter extends AbstractEntityIdHttpMessageConverter<Object> {
    public ListMessageConverter() {
        super(UniProtMediaType.LIST_MEDIA_TYPE);
    }

    @Override
    protected void writeEntity(String entity, OutputStream outputStream) throws IOException {
        outputStream.write((entity + "\n").getBytes());
    }
}
