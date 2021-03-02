package org.uniprot.api.uniprotkb.output.converter;

import static org.uniprot.api.rest.output.converter.ConverterConstants.UNIPROTKB_XML_CONTEXT;
import static org.uniprot.api.rest.output.converter.ConverterConstants.UNIPROTKB_XML_FOOTER;
import static org.uniprot.api.rest.output.converter.ConverterConstants.UNIPROTKB_XML_HEADER;

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
    protected Marshaller getMarshaller() {
        return createMarshaller();
    }

    @Override
    protected String getFooter() {
        return UNIPROTKB_XML_FOOTER;
    }

    @Override
    protected String getHeader() {
        return UNIPROTKB_XML_HEADER;
    }
}
