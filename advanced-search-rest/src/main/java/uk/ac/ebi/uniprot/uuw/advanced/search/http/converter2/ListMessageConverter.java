package uk.ac.ebi.uniprot.uuw.advanced.search.http.converter2;

import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.MessageConverterContext;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.UniProtMediaType;

import java.io.IOException;
import java.io.OutputStream;

public class ListMessageConverter extends AbstractUUWHttpMessageConverter<MessageConverterContext, String> {
    public ListMessageConverter() {
        super(UniProtMediaType.LIST_MEDIA_TYPE);
    }

    @Override
    protected boolean supports(Class<?> aClass) {
        if (MessageConverterContext.class.isAssignableFrom(aClass)) {
//            aClass.getField("resource")
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void writeEntity(String entity, OutputStream outputStream) throws IOException {
        outputStream.write((entity + "\n").getBytes());
    }
}
