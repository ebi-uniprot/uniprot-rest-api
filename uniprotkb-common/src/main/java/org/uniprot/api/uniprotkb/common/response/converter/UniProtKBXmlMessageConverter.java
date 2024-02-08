package org.uniprot.api.uniprotkb.common.response.converter;

import static org.uniprot.api.rest.output.converter.ConverterConstants.COPYRIGHT_TAG;
import static org.uniprot.api.rest.output.converter.ConverterConstants.UNIPROTKB_XML_CLOSE_TAG;
import static org.uniprot.api.rest.output.converter.ConverterConstants.UNIPROTKB_XML_CONTEXT;
import static org.uniprot.api.rest.output.converter.ConverterConstants.UNIPROTKB_XML_SCHEMA;
import static org.uniprot.api.rest.output.converter.ConverterConstants.XML_DECLARATION;

import java.io.IOException;
import java.io.OutputStream;

import org.uniprot.api.common.concurrency.Gatekeeper;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.converter.AbstractXmlMessageConverter;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.xml.jaxb.uniprot.Entry;
import org.uniprot.core.xml.uniprot.UniProtEntryConverter;

import jakarta.xml.bind.Marshaller;

public class UniProtKBXmlMessageConverter
        extends AbstractXmlMessageConverter<UniProtKBEntry, Entry> {
    private final ThreadLocal<UniProtEntryConverter> XML_CONVERTER = new ThreadLocal<>();

    public UniProtKBXmlMessageConverter() {
        this(null);
    }

    public UniProtKBXmlMessageConverter(Gatekeeper downloadGatekeeper) {
        super(UniProtKBEntry.class, UNIPROTKB_XML_CONTEXT, downloadGatekeeper);
    }

    @Override
    protected void before(
            MessageConverterContext<UniProtKBEntry> context, OutputStream outputStream)
            throws IOException {
        super.before(context, outputStream);
        XML_CONVERTER.set(new UniProtEntryConverter());
    }

    @Override
    protected Entry toXml(UniProtKBEntry entity) {
        return XML_CONVERTER.get().toXml(entity);
    }

    @Override
    protected void writeEntity(UniProtKBEntry entity, OutputStream outputStream)
            throws IOException {
        if (entity.isActive()) {
            super.writeEntity(entity, outputStream);
        }
    }

    @Override
    protected Marshaller getMarshaller() {
        return createMarshaller();
    }

    @Override
    protected String getFooter() {
        return COPYRIGHT_TAG + UNIPROTKB_XML_CLOSE_TAG;
    }

    @Override
    protected String getHeader() {
        return XML_DECLARATION + UNIPROTKB_XML_SCHEMA;
    }

    @Override
    protected void cleanUp() {
        super.cleanUp();
        XML_CONVERTER.remove();
    }
}
