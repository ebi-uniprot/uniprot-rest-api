package uk.ac.ebi.uniprot.api.rest.output.converter;


import uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType;

import java.io.IOException;
import java.io.OutputStream;

public class ListMessageConverter extends AbstractEntityIdHttpMessageConverter<String> {
    public ListMessageConverter() {
        super(UniProtMediaType.LIST_MEDIA_TYPE, String.class);
    }

    @Override
    protected void writeEntity(String entity, OutputStream outputStream) throws IOException {
        outputStream.write((entity + "\n").getBytes());
    }

}
