package uk.ac.ebi.uniprot.rest.output.converter;


import uk.ac.ebi.uniprot.rest.output.UniProtMediaType;

import java.io.IOException;
import java.io.OutputStream;

public class ListMessageConverter extends AbstractEntityIdHttpMessageConverter<Object> {
    public ListMessageConverter() {
        super(UniProtMediaType.LIST_MEDIA_TYPE);
    }

    @Override
    protected void writeEntity(String entity, OutputStream outputStream) throws IOException {
        outputStream.write((entity + "\n").getBytes());
    }
}
