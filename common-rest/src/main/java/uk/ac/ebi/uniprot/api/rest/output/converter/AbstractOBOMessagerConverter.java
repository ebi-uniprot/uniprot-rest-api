package uk.ac.ebi.uniprot.api.rest.output.converter;

import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.writer.OBOFormatWriter;
import uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;

import java.io.*;


public abstract class AbstractOBOMessagerConverter<T> extends AbstractEntityHttpMessageConverter<T> {

    private final OBOFormatWriter oboFormatWriter;

    public AbstractOBOMessagerConverter(Class<T> messageConverterEntryClass) {
        super(UniProtMediaType.OBO_MEDIA_TYPE, messageConverterEntryClass);
        this.oboFormatWriter = new OBOFormatWriter();
    }

    abstract protected Frame getTermFrame(T entity);

    abstract protected Frame getHeaderFrame();

    @Override
    protected void before(MessageConverterContext<T> context, OutputStream outputStream) throws IOException {
        Frame headerFrame = getHeaderFrame();
        StringWriter out = new StringWriter();
        this.oboFormatWriter.writeHeader(headerFrame, new PrintWriter(out), null);
        outputStream.write(out.getBuffer().toString().getBytes());
    }

    @Override
    protected void writeEntity(T entity, OutputStream outputStream) throws IOException {
        Frame termFrame = getTermFrame(entity);
        StringWriter out = new StringWriter();
        this.oboFormatWriter.write(termFrame, new PrintWriter(out), null);
        outputStream.write(out.getBuffer().toString().getBytes());
    }
}

