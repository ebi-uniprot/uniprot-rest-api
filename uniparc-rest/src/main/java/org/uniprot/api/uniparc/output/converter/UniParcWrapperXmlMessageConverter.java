package org.uniprot.api.uniparc.output.converter;

import org.uniprot.api.rest.output.converter.AbstractXmlMessageConverter;
import org.uniprot.api.uniparc.model.UniParcEntryWrapper;
import org.uniprot.core.xml.jaxb.uniparc.Entry;
import org.uniprot.core.xml.uniparc.UniParcEntryConverter;

import javax.xml.bind.Marshaller;

import static org.uniprot.api.rest.output.converter.ConverterConstants.UNIPARC_XML_CONTEXT;
import static org.uniprot.api.rest.output.converter.ConverterConstants.UNIPARC_XML_FOOTER;
import static org.uniprot.api.rest.output.converter.ConverterConstants.UNIPARC_XML_HEADER;

/**
 * @author sahmad
 * @created 24/03/2021
 */
public class UniParcWrapperXmlMessageConverter extends AbstractXmlMessageConverter<UniParcEntryWrapper, Entry> {
    private final UniParcEntryConverter converter;

    private String header;

    public UniParcWrapperXmlMessageConverter() {
        super(UniParcEntryWrapper.class, UNIPARC_XML_CONTEXT);
        converter = new UniParcEntryConverter();
        header = UNIPARC_XML_HEADER;
    }

    @Override
    protected String getHeader() {
        return header;
    }

    @Override
    protected Entry toXml(UniParcEntryWrapper entity) {

        return converter.toXml(entity.getEntry());
    }

    @Override
    protected Marshaller getMarshaller() {
        return createMarshaller();
    }

    @Override
    protected String getFooter() {
        return UNIPARC_XML_FOOTER;
    }
}

