package org.uniprot.api.rest.output.converter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.Objects;

import org.obolibrary.oboformat.model.Clause;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.parser.OBOFormatConstants;
import org.obolibrary.oboformat.writer.OBOFormatWriter;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;

public abstract class AbstractOBOMessageConverter<T> extends AbstractEntityHttpMessageConverter<T> {

    private final OBOFormatWriter oboFormatWriter;

    public AbstractOBOMessageConverter(Class<T> messageConverterEntryClass) {
        this(messageConverterEntryClass, null);
    }

    public AbstractOBOMessageConverter(
            Class<T> messageConverterEntryClass, Gatekeeper downloadGatekeeper) {
        super(UniProtMediaType.OBO_MEDIA_TYPE, messageConverterEntryClass, downloadGatekeeper);
        this.oboFormatWriter = new OBOFormatWriter();
    }

    protected abstract Frame getTermFrame(T entity);

    protected abstract String getHeaderNamespace();

    protected Frame getTypeDefStanza() {
        return null;
    }

    public Frame getHeaderFrame() {
        Frame headerFrame = new Frame(Frame.FrameType.HEADER);
        headerFrame.addClause(
                new Clause(OBOFormatConstants.OboFormatTag.TAG_FORMAT_VERSION, "1.2"));
        headerFrame.addClause(
                new Clause(
                        OBOFormatConstants.OboFormatTag.TAG_DATE,
                        OBOFormatConstants.headerDateFormat().format(new Date())));
        headerFrame.addClause(
                new Clause(
                        OBOFormatConstants.OboFormatTag.TAG_DEFAULT_NAMESPACE,
                        getHeaderNamespace()));
        return headerFrame;
    }

    @Override
    protected void before(MessageConverterContext<T> context, OutputStream outputStream)
            throws IOException {

        writeHeader(outputStream);
        writeTypeDef(outputStream);
    }

    @Override
    protected void writeEntity(T entity, OutputStream outputStream) throws IOException {
        Frame termFrame = getTermFrame(entity);
        StringWriter out = new StringWriter();
        this.oboFormatWriter.write(termFrame, new PrintWriter(out), null);
        outputStream.write(out.getBuffer().toString().getBytes());
    }

    private void writeHeader(OutputStream outputStream) throws IOException {
        Frame headerFrame = getHeaderFrame();
        StringWriter out = new StringWriter();
        this.oboFormatWriter.writeHeader(headerFrame, new PrintWriter(out), null);
        outputStream.write(out.getBuffer().toString().getBytes());
    }

    private void writeTypeDef(OutputStream outputStream) throws IOException {
        Frame typeDefStanza = getTypeDefStanza();
        if (Objects.nonNull(typeDefStanza)) {
            StringWriter out = new StringWriter();
            this.oboFormatWriter.write(typeDefStanza, new PrintWriter(out), null);
            outputStream.write(out.getBuffer().toString().getBytes());
        }
    }
}
