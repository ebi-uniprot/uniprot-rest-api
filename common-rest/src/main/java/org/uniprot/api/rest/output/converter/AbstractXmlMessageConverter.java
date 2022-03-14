package org.uniprot.api.rest.output.converter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.springframework.http.MediaType;
import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.rest.output.context.MessageConverterContext;

import com.sun.xml.bind.marshaller.DataWriter;

/**
 * @author jluo
 * @date: 2 May 2019
 */
public abstract class AbstractXmlMessageConverter<T, X>
        extends AbstractEntityHttpMessageConverter<T> {

    private final JAXBContext jaxbContext;

    public AbstractXmlMessageConverter(Class<T> messageConverterEntryClass, String context) {
        this(messageConverterEntryClass, context, null);
    }

    public AbstractXmlMessageConverter(
            Class<T> messageConverterEntryClass, String context, Gatekeeper downloadGatekeeper) {
        super(MediaType.APPLICATION_XML, messageConverterEntryClass, downloadGatekeeper);
        try {
            jaxbContext = JAXBContext.newInstance(context);
        } catch (JAXBException e) {
            throw new RuntimeException("JAXB initialisation failed", e);
        }
    }

    protected abstract X toXml(T entity);

    protected abstract Marshaller getMarshaller();

    protected abstract String getFooter();

    protected abstract String getHeader();

    @Override
    protected void before(MessageConverterContext<T> context, OutputStream outputStream)
            throws IOException {
        outputStream.write(getHeader().getBytes());
    }

    @Override
    protected void writeEntity(T entity, OutputStream outputStream) throws IOException {
        try {
            X entry = toXml(entity);
            Writer out = new OutputStreamWriter(outputStream);
            DataWriter writer = new DataWriter(out, "UTF-8");
            writer.setIndentStep("  ");
            getMarshaller().marshal(entry, writer);
            writer.characters("\n");
            writer.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void after(MessageConverterContext<T> context, OutputStream outputStream)
            throws IOException {
        outputStream.write(getFooter().getBytes());
    }

    protected Marshaller createMarshaller() {
        try {
            Marshaller contextMarshaller = jaxbContext.createMarshaller();
            contextMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            contextMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            return contextMarshaller;
        } catch (Exception e) {
            throw new RuntimeException("JAXB marshaller creation failed", e);
        }
    }
}
