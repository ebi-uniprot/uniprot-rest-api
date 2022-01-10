package org.uniprot.api.uniprotkb.output.converter;

import static org.uniprot.api.rest.output.converter.ConverterConstants.COPYRIGHT_TAG;
import static org.uniprot.api.rest.output.converter.ConverterConstants.UNIPROTKB_XML_CLOSE_TAG;
import static org.uniprot.api.rest.output.converter.ConverterConstants.UNIPROTKB_XML_CONTEXT;
import static org.uniprot.api.rest.output.converter.ConverterConstants.UNIPROTKB_XML_SCHEMA;
import static org.uniprot.api.rest.output.converter.ConverterConstants.XML_DECLARATION;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.bind.Marshaller;

import org.uniprot.api.rest.output.converter.AbstractXmlMessageConverter;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.xml.jaxb.uniprot.Entry;
import org.uniprot.core.xml.uniprot.UniProtEntryConverter;

public class UniProtKBXmlMessageConverter
        extends AbstractXmlMessageConverter<UniProtKBEntry, Entry> {
    private final UniProtEntryConverter converter;

    public UniProtKBXmlMessageConverter() {
        super(UniProtKBEntry.class, UNIPROTKB_XML_CONTEXT);
        converter = new UniProtEntryConverter();
    }

    @Override
    protected Entry toXml(UniProtKBEntry entity) {
        return converter.toXml(entity);
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
}
