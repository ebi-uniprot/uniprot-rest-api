package org.uniprot.api.rest.output.converter;

import java.io.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.springframework.http.MediaType;
import org.uniprot.api.rest.output.context.MessageConverterContext;

import com.sun.xml.bind.marshaller.DataWriter;

/**
 * @author jluo
 * @date: 2 May 2019
 */
public abstract class AbstractXmlMessageConverter<T, X>
        extends AbstractEntityHttpMessageConverter<T> {

    public AbstractXmlMessageConverter(Class<T> messageConverterEntryClass) {
        super(MediaType.APPLICATION_XML, messageConverterEntryClass);
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
        outputStream.write(getXmlString(entity).getBytes());
    }

    @Override
    protected void after(MessageConverterContext<T> context, OutputStream outputStream)
            throws IOException {
        outputStream.write(getFooter().getBytes());
    }

    protected Marshaller createMarshaller(String context) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(context);
            Marshaller contextMarshaller = jaxbContext.createMarshaller();
            contextMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            contextMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            return contextMarshaller;
        } catch (Exception e) {
            throw new RuntimeException("JAXB initialisation failed", e);
        }
    }

    private String getXmlString(T uniProtEntry) {
        try {
            X entry = toXml(uniProtEntry);
            StringWriter xmlString = new StringWriter();
            Writer out = new BufferedWriter(xmlString);
            DataWriter writer = new DataWriter(out, "UTF-8");
            writer.setIndentStep("  ");
            getMarshaller().marshal(entry, writer);
            writer.characters("\n");
            writer.flush();
            return xmlString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
